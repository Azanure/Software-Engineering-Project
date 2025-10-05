public class BitPackingOverlap implements BitPacking {
    @Override
    public BitPackedArray compress(int[] array) {
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

        int[] out = new int[words]; // Nouveau array compressé (les words)

        for (int i = 0; i < n; i++) {
            int start = i * k; // position de début (en bits) dans le flux
            int w = start / 32; // index du mot 32 bits (start / 32)
            int off = start % 32; // décalage dans ce mot (start % 32)

            int first = Math.min(32 - off, k); // bits qui tiennent dans le mot courant
            int rest = k - first; // bits qui débordent dans le mot suivant

            // partie 1 dans out[w], alignée à 'off'
            out[w] = out[w] | (array[i] << off);

            // partie 2 éventuelle dans out[w + 1], à partir du bit 0
            if (rest > 0) {
                // ici on a déjà consommé 'first' bits, on pousse le reste
                out[w + 1] = out[w + 1] | (array[i] >>> first);
            }
        }

        return new BitPackedArray(n, k, out, "overlap", this);
    }

    @Override
    public void decompress(int[] data, int k, int n, int[] output) {
        if (data == null) {
            throw new IllegalArgumentException("Le tableau compressé est null");
        }
        else if( output == null) {
            throw new IllegalArgumentException("Le tableau de sortie ne peut pas être null");
        }
        else if (output.length < n) {
            throw new IllegalArgumentException("Le tableau de sortie est trop petit: " + output.length + " < " + n);
        }

        for (int i = 0; i < n; i++) {
            int start = i * k; // position de début (en bits) dans le flux
            int w = start / 32; // index du mot 32 bits (start / 32)
            int off = start % 32; // décalage dans ce mot (start % 32)

            int first = Math.min(32 - off, k); // bits qui tiennent dans le mot courant
            int rest = k - first; // bits qui débordent dans le mot suivant

            // partie 1 dans data[w], alignée à 'off'
            output[i] = (data[w] >>> off) & ((1 << first) - 1);

            // partie 2 éventuelle dans data[w + 1], à partir du bit 0
            if (rest > 0) {
                output[i] |= (data[w + 1] & ((1 << rest) - 1)) << first;
            }
        }
    }

    @Override
    public int get(int[] data, int k, int n, int i) {
        if (i < 0 || i >= n) {
            throw new IndexOutOfBoundsException("Index hors limites: " + i);
        }

        int start = i * k;
        int w = start / 32; 
        int off = start % 32;

        int first = Math.min(32 - off, k); 
        int rest = k - first; 

        int value = (data[w] >>> off) & ((1 << first) - 1);
        if (rest > 0) {
            value = value | (data[w + 1] & ((1 << rest) - 1)) << first;
        }

        return value;
    }
}