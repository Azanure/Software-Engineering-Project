public class BitPackingOverlap implements BitPacking {
    @Override
    public int[] compress(int[] array) {
        final int n = (array == null) ? 0 : array.length; // taille de l'array

        if (array == null) {
            throw new IllegalArgumentException("Le tableau ne peut pas être null");
        }

        for (int v : array) {
            if (v < 0) {
                throw new IllegalArgumentException("Les valeurs négatives ne sont pas supportées");
            }
        }

        int max = 0;
        for (int v : array) {
            if (v > max) {
                max = v;
            }
        }
        int k = (array.length == 0) ? 1 : Math.max(1, 32 - Integer.numberOfLeadingZeros(max));
        if (k > 31) {
            throw new IllegalArgumentException("La valeur maximale du tableau dépasse 31 bits soit 2^31 - 1");
        }

        int totalBits = n * k; // nombre total de bits nécessaires
        int words = totalBits / 32; // nombre de mots 32 bits nécessaires arrondi au supérieur
        if (totalBits % 32 != 0) {
            words += 1;
        }
        if (words > Integer.MAX_VALUE - 2) {
            throw new OutOfMemoryError("La sortie est trop grande");
        }

        int[] out = new int[2 + words]; // Nouveau array avec 1) n, 2) k, puis le array compressé (les words)
        out[0] = n;
        out[1] = k;

        for (int i = 0; i < n; i++) {
            int start = i * k; // position de début (en bits) dans le flux
            int w = start / 32; // index du mot 32 bits (start / 32)
            int off = start % 32; // décalage dans ce mot (start % 32)

            int first = Math.min(32 - off, k); // bits qui tiennent dans le mot courant
            int rest = k - first; // bits qui débordent dans le mot suivant

            // partie 1 dans out[2 + w], alignée à 'off'
            out[2 + w] = out[2 + w] | (array[i] << off);

            // partie 2 éventuelle dans out[2 + w + 1], à partir du bit 0
            if (rest > 0) {
                // ici on a déjà consommé 'first' bits, on pousse le reste
                out[2 + w + 1] = out[2 + w + 1] | (array[i] >>> first);
            }
        }

        return out;
    }

    @Override
    public int[] decompressed(int[] compressedArray) {
        return new int[0];
    }

    @Override
    public int get(int i) {
        return 0;
    }

}