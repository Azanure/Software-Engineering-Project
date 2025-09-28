public interface BitPacking {
    int[] compress(int[] array);
    int[] decompressed(int[] compressedArray);
    int get(int i);
}
