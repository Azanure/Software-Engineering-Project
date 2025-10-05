public final class BitPackedArray {
    private final int n;
    private final int k;
    private final int[] data;
    private final String type; // Type de compression utilisé
    private final BitPacking packingType; // Instance de la stratégie de compression

    public BitPackedArray(int n, int k, int[] data, String type, BitPacking packingType) {
        this.n = n;
        this.k = k;
        this.data = data;
        this.type = type;
        this.packingType = packingType;
    }

    public int getSize() { return n; }
    public int getBitsPerValue() { return k; }
    public String getType() { return type; }
    public int[] getData() { return data; }

    public int get(int index) {
        if (index < 0 || index >= n) throw new IndexOutOfBoundsException("Index hors limites: " + index);
        switch (this.type) {
            case "overlap":
                return this.packingType.get(this.data, this.k, this.n, index);
            case "nooverlap":
                return this.packingType.get(this.data, this.k, this.n, index);
            case "overflow":
                return this.packingType.get(this.data, this.k, this.n, index);
            default:
                throw new IllegalArgumentException("Type de compression inconnu: " + this.type);
        }
    }

    public void decompress(int[] output) {
        if (output == null) throw new IllegalArgumentException("Le tableau de sortie ne peut pas être null");
        if (output.length < this.n) throw new IllegalArgumentException("Le tableau de sortie est trop petit");
        switch (this.type) {
            case "overlap":
                this.packingType.decompress(this.data, this.k, this.n, output);
                break;
            case "nooverlap":
                this.packingType.decompress(this.data, this.k, this.n, output);
                break;
            case "overflow":
                this.packingType.decompress(this.data, this.k, this.n, output);
                break;
            default:
                throw new IllegalArgumentException("Type de compression inconnu: " + this.type);
        }    
    }
}