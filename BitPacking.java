public interface BitPacking {
    BitPackedArray compress(int[] array);
    void decompress(int[] data, int k, int n, int[] output);
    int get(int[] data, int k, int n,int i);
}
