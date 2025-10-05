public class BitPackingNoOverlap implements BitPacking {
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

        int intPerWord = 32 / k; // nombre d'entiers par mot 32 bits
        int words = n / intPerWord; // nombre de mots 32 bits nécessaires arrondi au supérieur
        if (n % intPerWord != 0) {
            words += 1;
        }
        if (words > Integer.MAX_VALUE - 2) {
            throw new OutOfMemoryError("La sortie est trop grande");
        }

        int[] out = new int[words]; // Nouveau array compressé (les words)

        for (int i = 0; i < n; i++) {
            int w = i / intPerWord; // index du mot 32 bits
            int off = (i % intPerWord) * k; // décalage dans ce mot

            out[w] = out[w] | (array[i] << off);
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

        int intPerWord = 32 / k;
        for (int i = 0; i < n; i++) {
            int w = i / intPerWord; // index du mot 32 bits
            int off = (i % intPerWord) * k; // décalage dans ce mot

            output[i] = (data[w] >>> off) & ((1 << k) - 1);

        }
    }

    @Override
    public int get(int[] data, int k, int n, int i) {
        if (i < 0 || i >= n) {
            throw new IndexOutOfBoundsException("Index hors limites: " + i);
        }

        int intPerWord = 32 / k;
        int w = i / intPerWord; // index du mot 32 bits
        int off = (i % intPerWord) * k; // décalage dans ce mot

        int value = (data[w] >>> off) & ((1 << k) - 1);

        return value;
    }
}
