public class BitPackingOverflow implements BitPacking {
    @Override
    public BitPackedArray compress(int[] array) {
        // Récupération des informations pour la compression
        BitPackingUtils.ArrayInfo arrayInfo = BitPackingUtils.verifyArray(array);
        final int n = arrayInfo.length();
        BitPackingOverflow.BestKResult values = findBestK(array, n);
        int k = values.bestK();
        int totalBits = values.bestTotalCost();
        int bitsPerElement = values.bestBitsPerElement();
        int bitsPerOverflow =  values.maxBits();

        int words = totalBits / 32; // Nombre de cases (mots) de 32 bits nécessaires arrondi au supérieur
        if (totalBits % 32 != 0) {
            words += 1;
        }

        int[] out = new int[words]; // Nouveau array compressé (les mots)
        int overflowIndex = 0;
        int entry; // Entrée à écrire (dans la zone principale ou overflow)

        for (int i = 0; i < n; i++) {
            int start = i * bitsPerElement; // Position de début (en bits) de l'élément dans le flux de bits (zone principale)
            int wordIndex = start / 32; // Index de la case de 32 bits dans le nouveau tableau
            int off = start % 32; // Décalage (en bits) de l'élément dans cette case

            int first = Math.min(32 - off, bitsPerElement); // Nombre de bits de l'élément qui tiennent dans la case courante
            int rest = bitsPerElement - first; // Nombre de bits de l'élément qui débordent dans la case suivante

            if (array[i] > (1 << k) - 1) { // array[i] > 2^k - 1
                // On écrit d'abord la valeur dans la zone overflow
                int startOverflow = n * bitsPerElement + overflowIndex * bitsPerOverflow; // Position de début (en bits) de l'élément dans le flux de bits (zone overflow)
                int wordIndexOverflow = startOverflow / 32; // Index de la case de 32 bits dans le nouveau tableau
                int offOverflow = startOverflow % 32; // Décalage (en bits) de l'élément dans cette case

                int firstOverflow = Math.min(32 - offOverflow, bitsPerOverflow); // Nombre de bits de l'élément qui tiennent dans la case courante
                int restOverflow = bitsPerOverflow - firstOverflow; // Nombre de bits de l'élément qui débordent dans la case suivante

                // Partie 1 dans out[wordIndexOverflow], alignée à 'offOverflow'
                out[wordIndexOverflow] = out[wordIndexOverflow] | ((array[i] & ((1 << firstOverflow) - 1)) << offOverflow);
                // Partie 2 éventuelle dans out[wordIndexOverflow + 1], à partir du bit 0
                if (restOverflow > 0) {
                    out[wordIndexOverflow + 1] = out[wordIndexOverflow + 1] | ((array[i] >>> firstOverflow) & ((1 << restOverflow) - 1));
                }

                // On crée l'entrée avec le bit de flag à 1 et l'index de l'overflow (zone principale)
                entry = (1 << (bitsPerElement - 1)) | (overflowIndex);

                overflowIndex++;
            } else {
                // On crée l'entrée avec le bit de flag à 0 et la valeur normale (zone principale)
                entry = (array[i] & ((1 << bitsPerElement) - 1));
            }

            // On écrit la valeur ou l'index avec le bit de flag dans la zone principale

            // Partie 1 dans out[wordIndex], alignée à 'off'
            out[wordIndex] = out[wordIndex] | (entry << off);
            // Partie 2 éventuelle dans out[wordIndex + 1], à partir du bit 0
            if (rest > 0) {
                out[wordIndex + 1] = out[wordIndex + 1] | (entry >>> first);
            }
        }
        return new BitPackedArray(n, bitsPerElement, bitsPerOverflow, out, BitPackedArray.compressionType.OVERFLOW, this);
    }

    @Override
    public void decompress(BitPackedArray packedArray, int[] output) {
        int n = packedArray.getSize();
        int bitsPerElement = packedArray.getBitsPerElement();
        int bitsPerOverflow = packedArray.getOverflowBits();
        int[] compressedData = packedArray.getCompressedData();

        for (int i = 0; i < n; i++) {
            int start = i * bitsPerElement; // Position de début (en bits) de l'élément dans le flux de bits (zone principale)
            int wordIndex = start / 32; // Index de la case de 32 bits dans le tableau compressé
            int off = start % 32; // Décalage (en bits) de l'élément dans cette case

            int first = Math.min(32 - off, bitsPerElement); // Nombre de bits de l'élément qui tiennent dans la case courante
            int rest = bitsPerElement - first; // Nombre de bits de l'élément qui débordent dans la case suivante

            int outputNumber; // Nombre extrait de la zone principale ou overflow

            // On récupère la valeur ou l"index avec le bit de flag dans la zone principale
            // Partie 1 dans compressedData[wordIndex], alignée à 'off'
            outputNumber = (compressedData[wordIndex] >>> off) & ((1 << first) - 1);
            // Partie 2 éventuelle dans compressedData[wordIndex + 1], à partir du bit 0
            if (rest > 0) {
                outputNumber |= (compressedData[wordIndex + 1] & ((1 << rest) - 1)) << first;
            }

            // On vérifie si la valeur est un index ou une valeur normale
            if ((outputNumber & (1 << (bitsPerElement - 1))) != 0) { // Bit de flag overflow est à 1
                int overflowIndex = outputNumber & ((1 << (bitsPerElement - 1)) - 1); // On extrait l'index de l'overflow
                int startOverflow = n * bitsPerElement + overflowIndex * bitsPerOverflow; // Position de début (en bits) de l'élément dans le flux de bits (zone overflow)
                int wOverflow = startOverflow / 32; // Index de la case de 32 bits dans le nouveau tableau
                int offOverflow = startOverflow % 32; // Décalage (en bits) de l'élément dans cette case

                int firstOverflow = Math.min(32 - offOverflow, bitsPerOverflow); // Nombre de bits de l'élément qui tiennent dans la case courante
                int restOverflow = bitsPerOverflow - firstOverflow; // Nombre de bits de l'élément qui débordent dans la case suivante

                // Partie 1 dans compressedData[wOverflow], alignée à 'offOverflow'
                output[i] = (compressedData[wOverflow] >>> offOverflow) & ((1 << firstOverflow) - 1);
                // Partie 2 éventuelle dans compressedData[wOverflow + 1], à partir du bit 0
                if (restOverflow > 0) {
                    output[i] |= (compressedData[wOverflow + 1] & ((1 << restOverflow) - 1)) << firstOverflow;
                }
            } else { // Bit de flag overflow est à 0
                output[i] = outputNumber & ((1 << (bitsPerElement - 1)) - 1);
            }
        }
    }

    @Override
    public int get(BitPackedArray packedArray, int index) {
        int n = packedArray.getSize();
        int bitsPerElement = packedArray.getBitsPerElement();
        int bitsPerOverflow = packedArray.getOverflowBits();
        int[] compressedData = packedArray.getCompressedData();

        int start = index * bitsPerElement; // Position de début (en bits) de l'élément dans le flux de bits (zone principale)
        int wordIndex = start / 32; // Index de la case de 32 bits dans le tableau compressé
        int off = start % 32; // Décalage (en bits) de l'élément dans cette case

        int first = Math.min(32 - off, bitsPerElement); // Nombre de bits de l'élément qui tiennent dans la case courante
        int rest = bitsPerElement - first; // Nombre de bits de l'élément qui débordent dans la case suivante

        // On récupère la valeur ou l"index avec le bit de flag dans la zone principale
        int outputNumber = (compressedData[wordIndex] >>> off) & ((1 << first) - 1);
        if (rest > 0) {
            outputNumber |= (compressedData[wordIndex + 1] & ((1 << rest) - 1)) << first;
        }

        // On vérifie si la valeur est un index ou une valeur normale
        if ((outputNumber & (1 << (bitsPerElement - 1))) != 0) { // Bit de flag overflow est à 1
            int overflowIndex = outputNumber & ((1 << (bitsPerElement - 1)) - 1);
            int startOverflow = n * bitsPerElement + overflowIndex * bitsPerOverflow;
            int wOverflow = startOverflow / 32;
            int offOverflow = startOverflow % 32;

            int firstOverflow = Math.min(32 - offOverflow, bitsPerOverflow);
            int restOverflow = bitsPerOverflow - firstOverflow;

            int overflowValue = (compressedData[wOverflow] >>> offOverflow) & ((1 << firstOverflow) - 1);
            if (restOverflow > 0) {
                overflowValue |= (compressedData[wOverflow + 1] & ((1 << restOverflow) - 1)) << firstOverflow;
            }
            return overflowValue;
        } else { // Bit de flag overflow est à 0
            return outputNumber & ((1 << (bitsPerElement - 1)) - 1);
        }
    }

    // Record pour stocker les résultats de findBestK
    public record BestKResult(int bestK, int bestTotalCost, int bestBitsPerElement, int maxBits) {}

    // Trouver le meilleur nombre de bits sur le quel compresser la zone principale de façon à minimiser le coût total en bits
    public static BestKResult findBestK(int[] array, int n) {
        int bestK = 1; // k optimal (nombre de bits de compression pour la zone principale (SANS LE BIT DE FLAG ET SANS PRENDRE EN COMPTE LES INDEX))
        int bestBitsPerElement = 1; // Nombre de bits par élément optimal pour la zone principale (EN PRENANT EN COMPTE LE BIT DE FLAG ET LES EVENTUELS BITS D'INDEX) (VALEUR QU'ON CHERCHE)
        int bestTotalCost = Integer.MAX_VALUE; // Meilleur coût total pour le tableau de compression (en bits)
        int maxBits = 0; // Nombre maximum de bits nécessaires pour compresser toutes les valeurs
        for (int v : array) {
            int bits = 32 - Integer.numberOfLeadingZeros(v);
            if (bits > maxBits) {
                maxBits = bits;
            }
        }

        for (int k = 1; k <= maxBits; k++) {
            // Calcul du nombre d'overflows pour ce k
            int nOverflow = 0;
            for (int v : array) {
                if (32 - Integer.numberOfLeadingZeros(v) > k) {
                    nOverflow++;
                }
            }

            int indexBits = (nOverflow > 0) ? (32 - Integer.numberOfLeadingZeros(nOverflow)) : 0; // Nombre de bits minimum nécessaires pour indexer les overflows
            int bitsPerElement = 1 + Math.max(k, indexBits); // Bits par élément dans la partie principale (on prend la plus grosse representation entre k et indexBits + 1 bit de flag)
            int overflowCost = nOverflow * maxBits; // Coût total de la partie overflow en bits (chacun de ces nombres sera compressé forcement sur maxBits bits)
            int totalCost = n * bitsPerElement + overflowCost; // Coût total en bits (= coût de la partie principale + coût de la partie overflow)

            System.out.printf("k=%d | indexBits=%d | bitsPerElement=%d | nOverflow=%d | nPrincipal=%d | maxBits=%d | totalCost=%d%n", k, indexBits, bitsPerElement, nOverflow, n-nOverflow, maxBits, totalCost); //debug

            // Mise à jour des meilleurs résultats si nécessaire
            if (totalCost < bestTotalCost) {
                bestTotalCost = totalCost;
                bestK = k;
                bestBitsPerElement = bitsPerElement;
            }
        }
        System.out.println("=> Best k = " + bestK + " (cost = " + bestTotalCost + ")"); //debug
        return new BestKResult(bestK, bestTotalCost, bestBitsPerElement, maxBits);
    }
}
