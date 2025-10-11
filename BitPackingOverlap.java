public class BitPackingOverlap implements BitPacking {
    @Override
    public BitPackedArray compress(int[] array) {
        BitPackingUtils.ArrayInfo arrayInfo = BitPackingUtils.verifyArray(array);
        final int n = arrayInfo.length();
        final int bitsPerElement = arrayInfo.maxBits();

        int totalBits = n * bitsPerElement; // Nombre total de bits nécessaires
        int words = totalBits / 32; // Nombre de cases (mots) de 32 bits nécessaires arrondi au supérieur
        if (totalBits % 32 != 0) {
            words += 1;
        }

        int[] output = new int[words]; // Nouveau array compressé (les mots)

        for (int i = 0; i < n; i++) {
            int start = i * bitsPerElement; // Position de début (en bits) de l'élément dans le flux de bits
            int wordIndex = start / 32; // Index de la case de 32 bits dans le nouveau tableau
            int off = start % 32; // Décalage (en bits) de l'élément dans cette case

            int first = Math.min(32 - off, bitsPerElement); // Nombre de bits de l'élément qui tiennent dans la case courante
            int rest = bitsPerElement - first; // Nombre de bits de l'élément qui débordent dans la case suivante

            // Partie 1 dans output[wordIndex], alignée à 'off'
            output[wordIndex] = output[wordIndex] | (array[i] << off);

            // Partie 2 éventuelle dans output[wordIndex + 1], à partir du bit 0
            if (rest > 0) {
                // ici on a déjà consommé 'first' bits, on pousse le reste
                output[wordIndex + 1] = output[wordIndex + 1] | (array[i] >>> first);
            }
        }

        return new BitPackedArray(n, bitsPerElement, 0, output, BitPackedArray.compressionType.OVERLAP, this);
    }

    @Override
    public void decompress(BitPackedArray packedArray, int[] output) {
        int n = packedArray.getSize();
        int bitsPerElement = packedArray.getBitsPerElement();
        int[] compressedData = packedArray.getCompressedData();

        for (int i = 0; i < n; i++) {
            int start = i * bitsPerElement; // Position de début (en bits) de l'élément dans le flux de bits
            int wordIndex = start / 32; // Index de la case de 32 bits dans le tableau compressé
            int off = start % 32; // Décalage (en bits) de l'élément dans cette case

            int first = Math.min(32 - off, bitsPerElement); // Nombre de bits de l'élément qui tiennent dans la case courante
            int rest = bitsPerElement - first; // Nombre de bits de l'élément qui débordent dans la case suivante

            // Partie 1 dans compressedData[wordIndex], alignée à 'off'
            output[i] = (compressedData[wordIndex] >>> off) & ((1 << first) - 1);

            // Partie 2 éventuelle dans compressedData[wordIndex + 1], à partir du bit 0
            if (rest > 0) {
                output[i] |= (compressedData[wordIndex + 1] & ((1 << rest) - 1)) << first;
            }
        }
    }

    @Override
    public int get(BitPackedArray packedArray, int index) {
        int bitsPerElement = packedArray.getBitsPerElement();
        int[] compressedData = packedArray.getCompressedData();

        int start = index * bitsPerElement;
        int wordIndex = start / 32;
        int off = start % 32;

        // On calcule le nombre de bits qui tiennent dans la case courante et ceux qui débordent
        int first = Math.min(32 - off, bitsPerElement);
        int rest = bitsPerElement - first;

        // On récupère les bits dans la case courante et ceux qui débordent ensuite
        int value = (compressedData[wordIndex] >>> off) & ((1 << first) - 1);
        if (rest > 0) {
            value = value | (compressedData[wordIndex + 1] & ((1 << rest) - 1)) << first;
        }
        return value;
    }
}