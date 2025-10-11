public final class BitPackingUtils {

    public record ArrayInfo(int length, int maxValue, int maxBits) {}

    // Vérification du tableau d'entrée avant compression
    public static ArrayInfo verifyArray(int[] array) {
        if (array == null) {
            throw new IllegalArgumentException("Le tableau à compresser ne peut pas être null.");
        }
        final int n = array.length;

        // Vérification que toutes les valeurs du tableau sont positives et trouver la valeur maximale
        int maxValue = 0;
        for (int v : array) {
            if (v < 0) {
                throw new IllegalArgumentException("Le tableau à compresser contient une valeur négative: " + v);
            }
            if (v > maxValue) maxValue = v;
        }

        // Vérification que la valeur maximale du tableau tient sur 31 bits (sans le bit de signe) car sinon dépasse la capacité d'un int signé
        int maxBits = (maxValue == 0) ? 1 : 32 - Integer.numberOfLeadingZeros(maxValue);
        if (maxBits > 31) {
            throw new IllegalArgumentException("La valeur maximale du tableau dépasse 31 bits (2^31-1).");
        }
        return new ArrayInfo(n, maxValue, maxBits);
    }

    // Vérification du tableau compressé avant décompression
    public static void verifyCompressedArray(BitPackedArray packedArray, int[] output) {
        if (packedArray == null || packedArray.getCompressedData() == null) {
            throw new IllegalArgumentException("Le tableau à décompresser ne peut pas être null.");
        }
        if (output == null) {
            throw new IllegalArgumentException("Le tableau de sortie ne peut pas être null.");
        }
        int n = packedArray.getSize();
        if (output.length < n) {
            throw new IllegalArgumentException("Le tableau de sortie est plus petit que le tableau original: " + output.length + " < " + n);
        }
    }

    // Vérification de l'index avant d'accéder à un élément
    public static void verifyIndex(BitPackedArray packedArray, int index) {
        if (packedArray == null || packedArray.getCompressedData() == null) {
            throw new IllegalArgumentException("Le tableau compressé ne peut pas être null.");
        }
        int n = packedArray.getSize();
        if (index < 0 || index >= n) {
            throw new IndexOutOfBoundsException("L'index est hors limites pour le tableau compressé: " + index);
        }
    }
}
