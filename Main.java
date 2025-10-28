import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Main {
    private static final int DEFAULT_WARMUP = 3;
    private static final int DEFAULT_RUNS = 5;

    public static void main(String[] args) {
        try (Scanner sc = new Scanner(System.in)) {
            println("\n=== Bit Packing ===");

            // 1) On choisit le mode de compression
            println("Choisissez le mode de compression :");
            println(" 1 - Overlap");
            println(" 2 - NoOverlap");
            println(" 3 - Overflow");
            print("Votre choix [1-3]: ");
            int packingChoice = readInt(sc, 1, 3);
            String packingType = switch (packingChoice) {
                case 1 -> "overlap";
                case 2 -> "nooverlap";
                case 3 -> "overflow";
                default -> throw new IllegalStateException("Choix invalide");
            };

            // 2) On paramètre les données
            println("\nGénérer un tableau :");
            println("  1 - Uniforme (valeurs bornées entre 0 et max)");
            println("  2 - Avec outliers (valeurs bornées entre 0 et max + quelques valeurs très grandes)");
            print("Votre choix [1-2]: ");
            int dataChoice = readInt(sc, 1, 2);

            print("Taille du tableau n (entre 1 et " + Integer.MAX_VALUE + ") (ex: 100000): ");
            int n = readInt(sc, 1, Integer.MAX_VALUE);

            int maxValue = 4095;
            int outlierEvery = 0; // 0 = pas d'outlier
            int outlierMax = 1 << 20;

            if (dataChoice == 1) {
                print("Valeur max (ex: 4095 pour ~12 bits) (ne pas dépasser 2^31-1) [par défaut = 4095]: ");
                String line = sc.nextLine().trim();
                if (!line.isEmpty()) {
                    try {
                        long val = Long.parseLong(line);
                        if (val > Integer.MAX_VALUE) {
                            println("Valeur trop grande, limitée à " + Integer.MAX_VALUE);
                            maxValue = Integer.MAX_VALUE;
                        } else if (val < 1) {
                            println("Valeur trop petite, limitée à 1");
                            maxValue = 1;
                        } else {
                            maxValue = (int) val;
                        }
                    } catch (NumberFormatException e) {
                        println("Entrée invalide. Valeur par défaut utilisée : 4095");
                        maxValue = 4095;
                    }
                } else {
                    print("Valeur max hors-outliers (ex: 63 pour ~6 bits) (ne pas dépasser 2^31-1) [par défaut = 63]: ");
                    String line1 = sc.nextLine().trim();
                    if (!line1.isEmpty()) {
                        try {
                            long val = Long.parseLong(line1);
                            if (val > Integer.MAX_VALUE) {
                                println("Valeur trop grande, limitée à " + Integer.MAX_VALUE);
                                maxValue = Integer.MAX_VALUE;
                            } else if (val < 1) {
                                println("Valeur trop petite, limitée à 1");
                                maxValue = 1;
                            } else {
                                maxValue = (int) val;
                            }
                        } catch (NumberFormatException e) {
                            println("Entrée invalide. Valeur par défaut utilisée : 63");
                            maxValue = 63;
                        }
                    }

                    print("Toutes les k positions, insérer un outlier (ex: 1000) [par défaut = 1000]: ");
                    String line2 = sc.nextLine().trim();
                    if (!line2.isEmpty()) {
                        try {
                            int value = Integer.parseInt(line2);
                            outlierEvery = Math.max(2, value);
                        } catch (NumberFormatException e) {
                            System.out.println("Entrée invalide, utilisation de la valeur par défaut 1000.");
                            outlierEvery = 1000;
                        }
                    } else {
                        outlierEvery = 1000;
                    }

                    print("Valeur max d'outlier (ex: 1048575 ≈ 20 bits) [par défaut = 1048576]: ");
                    String line3 = sc.nextLine().trim();
                    if (!line3.isEmpty()) {
                        try {
                            int value = Integer.parseInt(line3);
                            outlierMax = Math.max(maxValue + 1, value);
                        } catch (NumberFormatException e) {
                            System.out.println("Entrée invalide, utilisation de la valeur par défaut 1048576.");
                            outlierMax = 1 << 20;
                        }
                    } else {
                        outlierMax = 1 << 20;
                    }
                }
            }

            // 3) Paramètres de bench
            print("\nNombre de warmups (échauffements) [ENTER=" + DEFAULT_WARMUP + "]: ");
            Integer warmups = tryParseInt(sc.nextLine().trim());
            if (warmups == null)
                warmups = DEFAULT_WARMUP;

            print("Nombre de runs (mesures) [ENTER=" + DEFAULT_RUNS + "]: ");
            Integer runs = tryParseInt(sc.nextLine().trim());
            if (runs == null)
                runs = DEFAULT_RUNS;

            // 4) Génération des données
            int[] data = (dataChoice == 1)
                    ? genUniform(n, maxValue)
                    : genWithOutliers(n, maxValue, outlierEvery, outlierMax);

            // 5) Création de l'instance de BitPacking via factory
            BitPacking bitPacking = BitPackingFactory.createBitPacking(packingType);

            println("\n--- Lancement des benchmarks ---");
            benchWarmup(() -> bitPacking.compress(data), warmups);

            long[] compressTimes = new long[runs];
            BitPackedArray packed = null;
            for (int i = 0; i < runs; i++) {
                long t0 = System.nanoTime();
                packed = bitPacking.compress(data);
                long t1 = System.nanoTime();
                compressTimes[i] = t1 - t0;
            }

            // Validation et bench decompress
            int[] recovered = new int[packed.getSize()];
            // Première décompression pour valider
            packed.decompress(recovered);
            boolean ok = Arrays.equals(data, recovered);

            // Warmup de décompression (SANS lambda)
            for (int i = 0; i < warmups; i++) {
                packed.decompress(recovered);
            }
            long[] decompressTimes = new long[runs];
            for (int i = 0; i < runs; i++) {
                long t0 = System.nanoTime();
                packed.decompress(recovered);
                long t1 = System.nanoTime();
                decompressTimes[i] = t1 - t0;
            }

            // Bench get(i) aléatoire
            int size = packed.getSize();
            final int queries = Math.min(1_000_000, Math.max(100_000, size));

            // Warmup (sans lambda)
            for (int i = 0; i < warmups; i++) {
                randomGets(packed, size, 10_000); // on ignore le retour
            }

            // Mesure
            long t0 = System.nanoTime();
            long checksum = randomGets(packed, size, queries);
            long t1 = System.nanoTime();

            long getNs = t1 - t0;
            double nsPerGet = (double) getNs / queries;

            // 6) Affichage des résultats (médianes + infos)
            println("\n=== Résultats ===");
            println("Mode                : " + packingType);
            println("n                   : " + n);
            println("Données             : " + (dataChoice == 1
                    ? "uniformes (max=" + maxValue + ")"
                    : "outliers (max=" + maxValue + ", every=" + outlierEvery + ", outlierMax=" + outlierMax
                            + ")"));
            println("Warmups / Runs      : " + warmups + " / " + runs);
            println("Correctness         : " + (ok ? "OK ✅" : "❌ ERREUR (décompression != originale)"));

            println("\n--- Temps (médiane) ---");
            println("compress()          : " + prettyNs(median(compressTimes)));
            println("decompress()        : " + prettyNs(median(decompressTimes)));
            println("get(i) (moyenne)    : "
                    + String.format(Locale.ROOT, "%.2f ns / accès    (checksum=%d)", nsPerGet, checksum));

            println("\nFini. Merci !");
        }
    }

    // ========= Utilitaires =========

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
            acc += packed.get(idx); // <-- ADAPTER ICI si signature différente
        }
        return acc;
    }

    private static int[] genUniform(int n, int maxValue) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int[] a = new int[n];
        for (int i = 0; i < n; i++)
            a[i] = rnd.nextInt(maxValue + 1);
        return a;
    }

    private static int[] genWithOutliers(int n, int baseMax, int everyK, int outlierMax) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            if (everyK > 0 && i > 0 && (i % everyK == 0)) {
                a[i] = rnd.nextInt(outlierMax + 1);
            } else {
                a[i] = rnd.nextInt(baseMax + 1);
            }
        }
        return a;
    }

    private static int readInt(Scanner sc, int min, int max) {
        while (true) {
            String s = sc.nextLine().trim();
            try {
                int v = Integer.parseInt(s);
                if (v < min || v > max)
                    throw new NumberFormatException();
                return v;
            } catch (NumberFormatException e) {
                print("Entrée invalide. Recommencez [" + min + "-" + max + "]: ");
            }
        }
    }

    private static Integer tryParseInt(String s) {
        if (s == null || s.isEmpty())
            return null;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String prettyNs(long ns) {
        if (ns < 1_000)
            return ns + " ns";
        if (ns < 1_000_000)
            return String.format(Locale.ROOT, "%.3f µs", ns / 1_000.0);
        if (ns < 1_000_000_000)
            return String.format(Locale.ROOT, "%.3f ms", ns / 1_000_000.0);
        return String.format(Locale.ROOT, "%.3f s", ns / 1_000_000_000.0);
    }

    private static long median(long[] arr) {
        long[] copy = Arrays.copyOf(arr, arr.length);
        Arrays.sort(copy);
        int mid = copy.length / 2;
        return (copy.length % 2 == 0) ? (copy[mid - 1] + copy[mid]) / 2 : copy[mid];
    }

    private static void println(String s) {
        System.out.println(s);
    }

    private static void print(String s) {
        System.out.print(s);
    }

}
