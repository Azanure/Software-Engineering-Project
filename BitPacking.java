public interface BitPacking {
    BitPackedArray compress(int[] array);
    void decompress(BitPackedArray packedArray, int[] output);
    int get(BitPackedArray packedArray, int index);
}
