public class BitPackingFactory {
    public static BitPacking createBitPacking(String type) { //"Static" pour pouvoir appeler la méthode sans objet
        switch(type){
            case "overlap":
                return new BitPackingOverlap();
            case "nooverlap":
                return new BitPackingNoOverlap();
            case "overflow":
                return new BitPackingOverflow();
            default:
                throw new IllegalArgumentException("Type inconnu: " + type);
        }
    }
}
