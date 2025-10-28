public class BitPackingNoOverlap implements BitPacking {
    @Override
    public BitPackedArray compress(int[] array) {
        BitPackingUtils.ArrayInfo arrayInfo = BitPackingUtils.verifyArray(array);
        final int n = arrayInfo.length();
        final int bitsPerElement = arrayInfo.maxBits();

        int elementsPerWord = 32 / bitsPerElement; // Nombre d'entiers par entrée de 32 bits du tableau compressé
        int words = n / elementsPerWord; // Nombre de cases (mots) de 32 bits nécessaires arrondi au supérieur
        if (n % elementsPerWord != 0) {
            words += 1;
        }

        int[] output = new int[words]; // Nouveau array compressé (les mots)

        for (int i = 0; i < n; i++) {
            int wordIndex = i / elementsPerWord; // Index de la case de 32 bits dans le nouveau tableau
            int off = (i % elementsPerWord) * bitsPerElement; // Décalage (en bits) de l'élément dans cette case

            // On rajoute l'élément dans la case du tableau compressé, aligné à 'off'
            output[wordIndex] = output[wordIndex] | (array[i] << off);
        }
        return new BitPackedArray(n, bitsPerElement, 0, output, BitPackedArray.compressionType.NO_OVERLAP, this);
    }
    @Override
    public void decompress(BitPackedArray packedArray, int[] output) {
        int n = packedArray.getSize();
        int bitsPerElement = packedArray.getBitsPerElement();
        int[] compressedData = packedArray.getCompressedData();

        int elementsPerWord = 32 / bitsPerElement; // Nombre d'entiers par entrée de 32 bits du tableau compressé
        for (int i = 0; i < n; i++) {
            int wordIndex = i / elementsPerWord; // Index de la case de 32 bits dans le nouveau tableau
            int off = (i % elementsPerWord) * bitsPerElement; // Décalage (en bits) de l'élément dans cette case

            // On rajoute l'élément dans la case du tableau, aligné à 'off'
            output[i] = (compressedData[wordIndex] >>> off) & ((1 << bitsPerElement) - 1);
        }
    }

    @Override
    public int get(BitPackedArray packedArray, int index) {
        int bitsPerElement = packedArray.getBitsPerElement();
        int[] compressedData = packedArray.getCompressedData();

        int elementsPerWord = 32 / bitsPerElement;
        int wordIndex = index / elementsPerWord;
        int off = (index % elementsPerWord) * bitsPerElement;

        // On récupère les bits dans la case courante
        int value = (compressedData[wordIndex] >>> off) & ((1 << bitsPerElement) - 1);

        return value;
    }
}
