import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Main {

    // Couleurs ANSI et styles
    private static final String RESET  = "\u001B[0m"; // réinitialisation du style
    private static final String BOLD   = "\u001B[1m"; // écrire en gras
    private static final String BLUE   = "\u001B[34m";
    private static final String CYAN   = "\u001B[36m";
    private static final String GREEN  = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED    = "\u001B[31m";

    // État courant de la session
    private static BitPacking currentBitpacking = null; // instance du type de bitpacking choisi
    private static String currentBitpackingName = null; // nom du bitpacking choisi
    private static int[] currentArray = null;           // tableau original brut
    private static BitPackedArray currentPacked = null; // résultat compressé

    // Paramètres benchmark par défaut
    private static final int DEFAULT_WARMUP = 3;
    private static final int DEFAULT_RUNS = 5;

    public static void main(String[] args) {
        try (Scanner sc = new Scanner(System.in)) {

            boolean running = true;
            while (running) {
                clearScreen();
                showMenuHeader();
                showSessionState();
                showMenuOptions();
                print(CYAN + "Choix > " + RESET);

                int choice;
                try {
                    choice = Integer.parseInt(sc.nextLine().trim());
                } catch (Exception e) {
                    printError("Entrée invalide (pas un nombre).");
                    waitForEnter(sc);
                    continue;
                }

                switch (choice) {
                    case 1:
                        clearScreen();
                        handleChooseBitPacking(sc);
                        waitForEnter(sc);
                        break;
                    case 2:
                        clearScreen();
                        handleCreateArray(sc);
                        waitForEnter(sc);
                        break;
                    case 3:
                        clearScreen();
                        handleCompress();
                        waitForEnter(sc);
                        break;
                    case 4:
                        clearScreen();
                        handleGet(sc);
                        waitForEnter(sc);
                        break;
                    case 5:
                        clearScreen();
                        handleDecompressCheck();
                        waitForEnter(sc);
                        break;
                    case 6:
                        clearScreen();
                        handleBenchmark(sc);
                        waitForEnter(sc);
                        break;
                    case 0:
                        clearScreen();
                        printSectionHeader("Quitter");
                        println("Au revoir!");
                        running = false; // Fin du programme
                        break;
                    default:
                        printError("Choix inconnu.");
                        waitForEnter(sc);
                }
            }
        }
    }

    // ========================
    // 1. Choix du bitpacking
    // ========================

    private static void handleChooseBitPacking(Scanner sc) {
        printSectionHeader("Choisir le mode de compression");

        println(" 1 - overlap");
        println(" 2 - nooverlap");
        println(" 3 - overflow");
        print(CYAN + "Votre choix > " + RESET);
        String c = sc.nextLine().trim();

        switch (c) {
            case "1":
                currentBitpackingName = "overlap";
                break;
            case "2":
                currentBitpackingName = "nooverlap";
                break;
            case "3":
                currentBitpackingName = "overflow";
                break;
            default:
                printError("Entrée invalide, pas de changement de mode de compression.");
                return;
        }
        currentBitpacking = BitPackingFactory.createBitPacking(currentBitpackingName);

        // Quand on change de bitpacking, la compression actuelle devient invalide
        currentPacked = null;

        printSuccess("Mode de compression sélectionné: " + currentBitpackingName);
    }

    // ========================
    // 2. Création du tableau
    // ========================

    private static void handleCreateArray(Scanner sc) {
        printSectionHeader("Définir le tableau source");

        println(" 1 - Je tape moi-même les valeurs");
        println(" 2 - Génération automatique");
        print(CYAN + "Votre choix > " + RESET);
        String mode = sc.nextLine().trim();

        if (mode.equals("1")) {
            handleSetArrayManual(sc);
        } else if (mode.equals("2")) {
            handleGenerateArray(sc);
        } else {
            printError("Choix inconnu. Rien fait.");
        }
    }

    // --- 2.a Saisie manuelle du tableau par l'utilisateur
    private static void handleSetArrayManual(Scanner sc) {
        printSectionHeader("Saisie manuelle du tableau");

        println("Entrez les entiers séparés par des espaces.");
        println("Exemple: 1 2 3 1024 4 5 2048");
        print(CYAN + "> " + RESET);

        String line = sc.nextLine().trim();
        if (line.isEmpty()) {
            printError("Aucune valeur donnée.");
            return;
        }

        String[] parts = line.split("\\s+");
        int[] arr = new int[parts.length];

        try {
            for (int i = 0; i < parts.length; i++) {
                int v = Integer.parseInt(parts[i]);
                if (v < 0) {
                    printError("Les valeurs doivent être >= 0.");
                    return;
                }
                arr[i] = v;
            }
        } catch (NumberFormatException e) {
            printError("Au moins une valeur n'est pas un entier type int valide.");
            return;
        }

        currentArray = arr;
        currentPacked = null; // nouvelle data => compression invalide
        printSuccess("Tableau enregistré (" + currentArray.length + " éléments).");
    }

    // --- 2.b Génération auto (uniforme ou outliers)
    private static void handleGenerateArray(Scanner sc) {
        printSectionHeader("Génération automatique du tableau");

        println(" 1 - Distribution uniforme (0..max)");
        println(" 2 - Distribution avec outliers (petits nombres + gros pics périodiques)");
        print(CYAN + "Votre choix > " + RESET);
        String mode = sc.nextLine().trim();

        print("Taille du tableau n ? " + CYAN);
        int n = Integer.parseInt(sc.nextLine().trim());
        print(RESET);

        if (n <= 0) {
            printError("n doit être > 0.");
            return;
        }

        if (mode.equals("1")) {
            print("Valeur max ? " + CYAN);
            int maxVal = Integer.parseInt(sc.nextLine().trim());
            print(RESET);

            if (maxVal < 0) {
                printError("maxVal doit être >= 0.");
                return;
            }
            currentArray = genUniform(n, maxVal);

        } else if (mode.equals("2")) {
            print("Valeur max 'normale' ? " + CYAN);
            int baseMax = Integer.parseInt(sc.nextLine().trim());
            print(RESET);

            print("Un outlier tous les combien d'éléments (k) ? " + CYAN);
            int everyK = Integer.parseInt(sc.nextLine().trim());
            print(RESET);

            print("Valeur max des outliers ? " + CYAN);
            int outMax = Integer.parseInt(sc.nextLine().trim());
            print(RESET);

            if (baseMax < 0 || outMax < 0) {
                printError("Les bornes doivent être >= 0.");
                return;
            }
            if (everyK <= 0) {
                printError("everyK doit être > 0.");
                return;
            }

            currentArray = genWithOutliers(n, baseMax, everyK, outMax);

        } else {
            printError("Mode inconnu. Rien généré.");
            return;
        }

        currentPacked = null;
        printSuccess("Tableau généré (" + currentArray.length + " éléments).");
    }

    // ========================
    // 3. Compression
    // ========================

    private static void handleCompress() {
        printSectionHeader("Compression");

        if (currentBitpacking == null) {
            printError("Choisissez d'abord un bitpacking (option 1).");
            return;
        }
        if (currentArray == null) {
            printError("Définissez un tableau (option 2).");
            return;
        }

        currentPacked = currentBitpacking.compress(currentArray);

        printSuccess("Compression effectuée.");
        printInfo("Taille tableau originel : " + currentArray.length + " entiers");
        printInfo("Taille tableau compressé : " + currentPacked.getCompressedData().length);
        printInfo("k (bitsPerElement) = " + currentPacked.getBitsPerElement());
        printInfo("kOverflow (bitsPerElement overflow)(0 si pas d'overflow) = " + currentPacked.getOverflowBits());
    }

    // ========================
    // 4. Accès aléatoire get()
    // ========================

    private static void handleGet(Scanner sc) {
        printSectionHeader("Lecture aléatoire (get)");

        if (currentPacked == null) {
            printError("Il faut compresser d'abord (option 3).");
            return;
        }
        print("Index à lire ? " + CYAN);
        int idx = Integer.parseInt(sc.nextLine().trim());
        print(RESET);

        if (idx < 0 || idx >= currentPacked.getSize()) {
            printError("Index hors limites. Taille = " + currentPacked.getSize());
            return;
        }

        int value = currentPacked.get(idx);
        printSuccess("Valeur à l'index " + idx + " = " + value);
    }

    // ========================
    // 5. Décompression + vérif
    // ========================

    private static void handleDecompressCheck() {
        printSectionHeader("Décompression et vérification");

        if (currentPacked == null) {
            printError("Compresse d'abord (option 3).");
            return;
        }
        if (currentArray == null) {
            printError("Pas de tableau original pour vérifier.");
            return;
        }

        int n = currentPacked.getSize();
        int[] restored = new int[n];
        currentPacked.decompress(restored);

        boolean ok = Arrays.equals(restored, currentArray);

        if (ok) {
            printSuccess("Décompression réussie : Les tableaux sont identiques.");
        } else {
            printError("Décompression échouée : Les tableaux diffèrent.");
            println(YELLOW + "Exemple diff (premiers 10 éléments) :" + RESET);
            for (int i = 0; i < Math.min(n, 10); i++) {
                println("i=" + i + " original=" + currentArray[i] + " restored=" + restored[i]);
            }
        }
    }

    // ========================
    // 6. Benchmark
    // ========================

    private static void handleBenchmark(Scanner sc) {
        printSectionHeader("Benchmark");

        if (currentBitpacking == null) {
            printError("Choisissez un bitpacking (option 1).");
            return;
        }
        if (currentArray == null) {
            printError("Définissez un tableau (option 2).");
            return;
        }

        print("Combien de requêtes get() aléatoires pour la mesure ? " + CYAN);
        int q = Integer.parseInt(sc.nextLine().trim());
        print(RESET);

        if (q <= 0) {
            printError("Le nombre de requêtes doit être > 0.");
            return;
        }

        // 1) Benchmark compress()
        long[] compressTimes = new long[DEFAULT_RUNS];

        benchWarmup(() -> {
            currentBitpacking.compress(currentArray);
        }, DEFAULT_WARMUP);

        for (int r = 0; r < DEFAULT_RUNS; r++) {
            long t0 = System.nanoTime();
            BitPackedArray tmp = currentBitpacking.compress(currentArray);
            long t1 = System.nanoTime();
            compressTimes[r] = (t1 - t0);

            // garder le dernier pour la suite
            currentPacked = tmp;
        }

        // 2) Benchmark get()
        long[] getTimes = new long[DEFAULT_RUNS];
        long checksum = 0;
        for (int r = 0; r < DEFAULT_RUNS; r++) {
            long t0 = System.nanoTime();
            checksum += randomGets(currentPacked, currentPacked.getSize(), q);
            long t1 = System.nanoTime();
            getTimes[r] = (t1 - t0);
        }
        double avgGetNsPerAccess = (double) median(getTimes) / (double) q;

        // 3) Benchmark decompress()
        int n = currentPacked.getSize();
        int k = currentPacked.getBitsPerElement();
        int kOverflow = currentPacked.getOverflowBits();

        long[] decompressTimes = new long[DEFAULT_RUNS];

        benchWarmup(() -> {
            int[] buf = new int[n];
            currentPacked.decompress(buf);
        }, DEFAULT_WARMUP);

        for (int r = 0; r < DEFAULT_RUNS; r++) {
            int[] buf = new int[n];
            long t0 = System.nanoTime();
            currentPacked.decompress(buf);
            long t1 = System.nanoTime();
            decompressTimes[r] = (t1 - t0);
        }

        // Résumé
        println(BOLD + CYAN + "\n--- Résultats benchmark (" + currentBitpackingName + ") ---" + RESET);
        println("Taille tableau          : " + n + " éléments");
        println("bits per element (k)    : " + k);
        println("bits per elem. overflow : " + kOverflow);
        println("Temps compress() médian : " + prettyNs(median(compressTimes)));
        println("Temps decompress() méd. : " + prettyNs(median(decompressTimes)));
        println("Temps get() ~ns/accès   : " +
                String.format(Locale.ROOT, "%.2f ns (checksum=%d)", avgGetNsPerAccess, checksum));

        // Seuil de rentabilité
        int sizeBefore = currentArray.length * 4;
        int sizeAfter = currentPacked.getCompressedData().length * 4;
        double compressionRatio = (double) sizeBefore / sizeAfter;

        double compressMs = median(compressTimes) / 1_000_000.0;
        double decompressMs = median(decompressTimes) / 1_000_000.0;
        double tThresholdMs = (compressMs + decompressMs) / (compressionRatio - 1);

        println(String.format(Locale.ROOT,
            "Taille avant : %d o, après : %d o (ratio %.2fx)",
            sizeBefore, sizeAfter, compressionRatio));
        println(String.format(Locale.ROOT,
            "Compression profitable si latence réseau > %.3f ms", tThresholdMs));

    }

    // ========================
    // Outils benchmark / génération
    // ========================

    private static void benchWarmup(Runnable r, int warmups) {
        for (int i = 0; i < warmups; i++) {
            r.run();
        }
    }

    private static long randomGets(BitPackedArray packed, int n, int queries) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        long acc = 0;
        for (int i = 0; i < queries; i++) {
            int idx = rnd.nextInt(n);
            acc += packed.get(idx);
        }
        return acc;
    }

    private static int[] genUniform(int n, int maxValue) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = rnd.nextInt(maxValue + 1);
        }
        return a;
    }

    private static int[] genWithOutliers(int n, int baseMax, int everyK, int outMax) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            if (i > 0 && (i % everyK == 0)) {
                a[i] = rnd.nextInt(outMax + 1);
            } else {
                a[i] = rnd.nextInt(baseMax + 1);
            }
        }
        return a;
    }

    // ========================
    // AFFICHAGE DU MENU
    // ========================

    private static void showMenuHeader() {
        println(BLUE + "-------------------------------------------" + RESET);
        println(BOLD + CYAN + "         BITPACKING TOOL         " + RESET);
        println(BLUE + "-------------------------------------------" + RESET);
    }

    private static void showSessionState() {
        println(BOLD + "État courant :" + RESET);
        println("  Bitpacking actuel  : " +
                (currentBitpackingName == null ? YELLOW + "(aucun)" + RESET : GREEN + currentBitpackingName + RESET));
        println("  Array courant      : " +
                (currentArray == null ? YELLOW + "(aucun)" + RESET : GREEN + (currentArray.length + " éléments") + RESET));
        println("  Array compressé    : " +
                (currentPacked == null ? RED + "non" + RESET : GREEN + "oui" + RESET));
        println(BLUE + "-------------------------------------------" + RESET);
    }

    private static void showMenuOptions() {
        println(BOLD + "Actions :" + RESET);
        println(" 1 - Choisir l'algorithme (overlap / nooverlap / overflow)");
        println(" 2 - Définir le tableau source (saisie manuelle OU génération aléatoire)");
        println(" 3 - Compresser le tableau courant");
        println(" 4 - Lire un index i dans le tableau compressé (get)");
        println(" 5 - Décompresser et vérifier l'égalité avec l'original");
        println(" 6 - Lancer le benchmark complet");
        println(" 0 - Quitter");
        println(BLUE + "-------------------------------------------" + RESET);
    }

    // ========================
    // Helpers affichage / stats
    // ========================

    private static void waitForEnter(Scanner sc) {
        println(YELLOW + "\n(Appuyez sur Entrée pour continuer...)" + RESET);
        sc.nextLine();
    }

    // Efface l'écran (si le terminal supporte ANSI)
    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static void printSectionHeader(String title) {
        println(BLUE + "=== " + title + " ===" + RESET);
    }

    private static void printSuccess(String msg) {
        println(GREEN + "[OK] " + msg + RESET);
    }

    private static void printInfo(String msg) {
        println(YELLOW + "[i] " + msg + RESET);
    }

    private static void printError(String msg) {
        println(RED + "[X] " + msg + RESET);
    }

    private static String prettyNs(long ns) {
        if (ns < 1_000) {
            return ns + " ns";
        } else if (ns < 1_000_000) {
            double us = ns / 1_000.0;
            return String.format(Locale.ROOT, "%.3f µs", us);
        } else if (ns < 1_000_000_000L) {
            double ms = ns / 1_000_000.0;
            return String.format(Locale.ROOT, "%.3f ms", ms);
        } else {
            double s = ns / 1_000_000_000.0;
            return String.format(Locale.ROOT, "%.3f s", s);
        }
    }

    private static long median(long[] arr) {
        long[] copy = Arrays.copyOf(arr, arr.length);
        Arrays.sort(copy);
        int mid = copy.length / 2;
        if (copy.length % 2 == 0) {
            return (copy[mid - 1] + copy[mid]) / 2;
        } else {
            return copy[mid];
        }
    }

    private static void print(String s) {
        System.out.print(s);
    }

    private static void println(String s) {
        System.out.println(s);
    }
}
