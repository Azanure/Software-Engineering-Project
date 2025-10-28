# Projet de Génie Logiciel 2025  
## Compression de données pour accélérer la transmission

### Auteur  
**Cazacu Ion**
Université Côte d’Azur – Master Informatique
Octobre 2025  

---

## Objectif du projet  

L’objectif de ce projet est d’étudier et d’implémenter différentes techniques de **compression d’entiers** permettant de **réduire la quantité de données à transmettre** tout en conservant un **accès direct** à chaque élément du tableau original.

Le principe repose sur le **Bit Packing**, c’est-à-dire la représentation compacte des entiers sur le nombre minimal de bits nécessaires.  
Le projet comprend plusieurs variantes de compression, la gestion des **zones de débordement (overflow)**, ainsi qu’un **protocole de mesure de performance** pour évaluer la pertinence de la compression selon la latence du réseau.

---

## Fonctionnalités implémentées  

### 1. Bit Packing (compression de base)
Compression basée sur un nombre fixe de bits k :

- BitPackingNoOverlap : chaque entier est stocké entièrement dans un seul mot de 32 bits (aucun chevauchement entre entiers).  
- BitPackingOverlap : les entiers peuvent se chevaucher sur deux entiers consécutifs, ce qui augmente la densité de compression.

Chaque version implémente les fonctions :
- compress(int[] array) — compresse un tableau d’entiers  
- decompress(int[] data, int k, int n, int[] output) — décompresse les données  
- get(int[] data, int k, int n, int i) — permet d’accéder directement à l’élément i du tableau original  

### 2. Bit Packing avec zone de débordement (Overflow)
Implémenté dans BitPackingOverflow.java.  
Cette méthode optimise la compression lorsque certains entiers nécessitent beaucoup plus de bits que les autres.  
Le principe :
- Les valeurs « normales » sont compressées sur un petit nombre de bits k'  
- Les valeurs trop grandes sont placées dans une zone de débordement séparée  
- Un bit indicateur signale si la valeur est stockée directement ou dans la zone de débordement  

Cette approche réduit fortement la taille globale du tableau lorsque peu de valeurs sont extrêmes.

### 3. Fabrique (Factory Design Pattern)
Implémentée dans BitPackingFactory.java, elle permet de créer facilement la variante de compression souhaitée.  
Exemple :  
BitPacking packer = BitPackingFactory.create("overlap");

Les options disponibles sont :
- "nooverlap"
- "overlap"
- "overflow"

### 4. Fonctions utilitaires
Le fichier BitPackingUtils.java contient diverses fonctions d’aide :
- Manipulation de bits (masques, décalages, opérations logiques)
- Calcul du nombre minimal de bits k nécessaires pour représenter les valeurs
- Outils de mesure de temps d’exécution pour les benchmarks

### 5. Mesures de performance et benchmark
Le fichier Main.java met en œuvre un protocole complet de mesure des temps d’exécution :
- Mesure du temps moyen de compression, décompression, et accès direct (get)
- Calcul du temps de transmission total en fonction d’une latence donnée
- Détermination du temps de latence seuil à partir duquel la compression devient avantageuse  

Les mesures utilisent System.nanoTime() et sont moyennées sur plusieurs itérations pour garantir la précision.

---

## Utilisation

### 1. Compilation
javac *.java

### 2. Exécution
java Main

---

## Fonctionnement du programme principal (Main.java)

Le fichier Main.java constitue une interface utilisateur interactive en console permettant de manipuler les différents algorithmes de compression, de générer des tableaux, de tester la lecture aléatoire et de lancer des benchmarks complets.

Lors de l’exécution (java Main), un menu interactif s’affiche avec plusieurs choix numérotés. L’utilisateur navigue dans le programme en entrant le numéro correspondant à l’action souhaitée.
Toutes les actions sont réalisées pas à pas, avec affichage en couleur (ANSI) pour indiquer les états et les erreurs.

### Déroulement général :

1. Choix du type de compression (option 1)
   - L’utilisateur sélectionne le mode de compression parmi :
     - overlap : compression avec chevauchement entre entiers
     - nooverlap : compression stricte sans chevauchement
     - overflow : compression avec zone de débordement
   - Le programme crée automatiquement une instance via la fabrique BitPackingFactory.
   - Toute compression précédente est réinitialisée lorsqu’un nouveau mode est choisi.

2. Création du tableau source (option 2)
   - L’utilisateur peut :
     - Saisir manuellement ses propres entiers (exemple : 1 2 3 1024 4 5 2048)
     - Générer automatiquement un tableau selon deux distributions :
       - Uniforme : valeurs aléatoires entre 0 et une borne max donnée
       - Avec outliers : petites valeurs majoritaires, avec quelques valeurs extrêmes
   - Le tableau créé devient le jeu de données actif à compresser.

3. Compression du tableau (option 3)
   - Le programme compresse le tableau courant avec le mode sélectionné.
   - Il affiche :
     - La taille du tableau d’origine et du tableau compressé
     - Le nombre de bits utilisés (k et kOverflow si applicable)
     - Un message de confirmation en vert en cas de succès

4. Lecture d’un élément compressé (option 4)
   - Permet d’accéder à un élément précis sans décompresser tout le tableau.
   - L’utilisateur saisit un index entre 0 et n−1.
   - La valeur originale correspondante est affichée instantanément.
   - Cette option illustre la capacité d’accès direct aux données compressées.

5. Décompression et vérification (option 5)
   - Le programme décompresse le tableau compressé.
   - Il compare le résultat au tableau original.
   - Si une différence est détectée, un message d’erreur s’affiche.
   - Si tout est identique, un message de validation apparaît en vert.

6. Benchmark complet (option 6)
   - Le programme exécute plusieurs tests automatiques :
     - Mesure des temps médians pour compress(), decompress() et get()
     - Calcul du ratio de compression (taille avant/après)
     - Détermination du seuil de latence réseau où la compression devient rentable
   - Les résultats sont affichés en ms, µs ou ns selon le contexte.

7. Quitter (option 0)
   - Termine le programme proprement avec un message de fin.

### Interaction utilisateur

- Le menu principal propose les options suivantes :
  1 - Choisir l’algorithme (overlap / nooverlap / overflow)
  2 - Définir le tableau source
  3 - Compresser le tableau courant
  4 - Lire un index i dans le tableau compressé (get)
  5 - Décompresser et vérifier l’égalité avec l’original
  6 - Lancer le benchmark complet
  0 - Quitter

- Le programme affiche en permanence :
  - Le mode de compression actif
  - L’état du tableau (défini ou non)
  - Si les données sont déjà compressées

- Chaque étape est accompagnée d’un message explicatif et coloré :
  - Vert → succès
  - Rouge → erreur
  - Jaune → information

Ce fonctionnement rend le Main.java à la fois pédagogique et expérimental :
l’utilisateur peut créer ses propres jeux de données, comparer les performances des différentes méthodes de compression, et observer en temps réel les effets sur la taille et la vitesse de traitement.

---

## Protocole de mesure

Le protocole vise à mesurer de façon fiable le temps d’exécution de chaque fonction :
1. Chaque opération (compression, décompression, accès) est exécutée plusieurs fois.  
2. Les temps moyens sont calculés après élimination des valeurs d’échauffement (warm-up).  
3. Le temps total de transmission est estimé selon la formule :
   temps_total = latence + (taille_données / bande_passante)  
   La compression devient intéressante lorsque :
   t_compressé + transmission_compressée < t_non_compressé + transmission_non_compressée  

---

## Structure du projet

BitPackedArray.java                    → Structure contenant le tableau compressé et ses métadonnées  
BitPacking.java                        → Interface de compression  
BitPackingFactory.java                 → Fabrique pour choisir la méthode de compression  
BitPackingNoOverlap.java               → Compression sans chevauchement  
BitPackingOverlap.java                 → Compression avec chevauchement  
BitPackingOverflow.java                → Compression avec zone de débordement  
BitPackingUtils.java                   → Fonctions utilitaires de vérification
Main.java                              → Point d’entrée et protocole de benchmarks
Software Engineering Project 2025.txt  → Cahier des charges du projet  

---

## Tests et validation

- Vérification de la cohérence de la compression/décompression sur différents tableaux  
- Contrôle de la validité de la fonction get() (accès direct à un élément)  
- Comparaison entre tableau original et tableau décompressé  
- Mesure des performances pour chaque stratégie dans des conditions identiques  

---

## Livrables

- Code source : tous les fichiers .java  
- README.md : guide d’utilisation et documentation (ce fichier)  
- Rapport PDF : explications, conception, benchmarks et résultats
