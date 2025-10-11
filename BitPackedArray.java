public final class BitPackedArray {
    public enum compressionType{
        OVERLAP,
        NO_OVERLAP,
        OVERFLOW
    } // Types de compressions

    private final int n; // Nombre d'entiers compressés
    private final int bitsPerElement; // Nombre de bits par élement dans la zone principale
    private final int overflowBitsPerElement; // Nombre de bits par élement dans la zone overflow (pour compression overflow, est égal à 0 sinon)
    private final int[] compressedData; // Array avec données compressées
    private final compressionType packingType; // Type de compression utilisé
    private final BitPacking bitPackingInstance; // Instance du type de compression (BitPacking) utilisé

    public BitPackedArray(int n, int bitsPerElement, int overflowBitsPerElement, int[] compressedData, compressionType packingType, BitPacking bitPackingInstance) {
        this.n = n;
        this.bitsPerElement = bitsPerElement;
        this.overflowBitsPerElement = overflowBitsPerElement;
        this.compressedData = compressedData;
        this.packingType = packingType;
        this.bitPackingInstance = bitPackingInstance;
    }

    // Getters
    public int getSize() { return n; }
    public int getBitsPerElement() { return bitsPerElement; }
    public int getOverflowBits() { return overflowBitsPerElement; }
    public compressionType getType() { return packingType; }
    public int[] getCompressedData() { return compressedData; }

    // Méthodes de décompression
    public void decompress(int[] output) {
        BitPackingUtils.verifyCompressedArray(this, output);
        switch (this.packingType) {
            case OVERLAP:
                this.bitPackingInstance.decompress(this, output);
                break;
            case NO_OVERLAP:
                this.bitPackingInstance.decompress(this, output);
                break;
            case OVERFLOW:
                this.bitPackingInstance.decompress(this, output);
                break;
            default:
                throw new IllegalArgumentException("Le tableau a été compressé avec un BitPacking inconnu: " + this.packingType);
        }
    }

    // Méthodes d'accès aux éléments
    public int get(int index) {
        BitPackingUtils.verifyIndex(this, index);
        switch (this.packingType) {
            case OVERLAP:
                return this.bitPackingInstance.get(this, index);
            case NO_OVERLAP:
                return this.bitPackingInstance.get(this, index);
            case OVERFLOW:
                return this.bitPackingInstance.get(this, index);
            default:
                throw new IllegalArgumentException("Le tableau a été compressé avec un BitPacking inconnu: " + this.packingType);
        }
    }
}