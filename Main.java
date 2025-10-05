public class Main {
    public static void main(String[] args) {
        // Test
        int[] array = { 3, 5, 7, 15, 31, 63, 127 };
        String type = "nooverlap";
        BitPacking overlap = BitPackingFactory.createBitPacking(type);

        // Compression
        BitPackedArray compressed = overlap.compress(array);
        System.out.println("Compressed data: ");
        for (int value : compressed.getData()) {
            System.out.print(value + " ");
        }
        System.out.println();

        // Décompression
        int[] decompressed = new int[array.length];
        compressed.decompress(decompressed);
        System.out.println("Decompressed data: ");
        for (int value : decompressed) {
            System.out.print(value + " ");
        }
        System.out.println();

        // Get
        int indexToGet = 4;
        int valueAtIndex = overlap.get(compressed.getData(), compressed.getBitsPerValue(), compressed.getSize(),
                indexToGet);
        System.out.println("Value at index " + indexToGet + ": " + valueAtIndex);
    }
}
