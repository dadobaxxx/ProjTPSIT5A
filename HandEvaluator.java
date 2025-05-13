package com.ProjTPSIT5A;

import java.util.*;

public class HandEvaluator {
    private static final int HAND_SIZE = 5;

    public static class InvalidHandException extends IllegalArgumentException {
        public InvalidHandException(int expected, int actual) {
            super("[HAND] Combinazione non valida. Attese: " + expected + " carte, ricevute: " + actual);
        }
    }

    public static GameEngine.HandRank evaluateHand(List<GameEngine.Card> cards) {
        if (cards == null || cards.size() < 5) {
            throw new InvalidHandException(5, cards != null ? cards.size() : 0);
        }

        List<List<GameEngine.Card>> combinations = generateCombinations(cards, HAND_SIZE);
        GameEngine.HandRank bestRank = GameEngine.HandRank.HIGH_CARD;

        for (List<GameEngine.Card> combo : combinations) {
            GameEngine.HandRank currentRank = evaluateCombination(combo);
            if (currentRank.compareTo(bestRank) > 0) {
                bestRank = currentRank;
            }
        }
        return bestRank;
    }

    private static List<List<GameEngine.Card>> generateCombinations(List<GameEngine.Card> cards, int size) {
        List<List<GameEngine.Card>> result = new ArrayList<>();
        generateCombinations(cards, size, 0, new ArrayList<>(), result);
        return result;
    }

    private static void generateCombinations(List<GameEngine.Card> cards, int size, int start,
            List<GameEngine.Card> current, List<List<GameEngine.Card>> result) {
        if (current.size() == size) {
            result.add(new ArrayList<>(current));
            return;
        }

        for (int i = start; i < cards.size(); i++) {
            current.add(cards.get(i));
            generateCombinations(cards, size, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    private static GameEngine.HandRank evaluateCombination(List<GameEngine.Card> combo) {
        combo.sort((c1, c2) -> c2.rank.value - c1.rank.value);

        if (isRoyalFlush(combo))
            return GameEngine.HandRank.ROYAL_FLUSH;
        if (isStraightFlush(combo))
            return GameEngine.HandRank.STRAIGHT_FLUSH;
        if (isFourOfAKind(combo))
            return GameEngine.HandRank.FOUR_OF_A_KIND;
        if (isFullHouse(combo))
            return GameEngine.HandRank.FULL_HOUSE;
        if (isFlush(combo))
            return GameEngine.HandRank.FLUSH;
        if (isStraight(combo))
            return GameEngine.HandRank.STRAIGHT;
        if (isThreeOfAKind(combo))
            return GameEngine.HandRank.THREE_OF_A_KIND;
        if (isTwoPair(combo))
            return GameEngine.HandRank.TWO_PAIR;
        if (isOnePair(combo))
            return GameEngine.HandRank.ONE_PAIR;

        return GameEngine.HandRank.HIGH_CARD;
    }

    private static boolean isRoyalFlush(List<GameEngine.Card> combo) {
        return isStraightFlush(combo) && combo.get(0).rank == GameEngine.Rank.ACE &&
                combo.get(4).rank == GameEngine.Rank.TEN;
    }

    private static boolean isStraightFlush(List<GameEngine.Card> combo) {
        return isFlush(combo) && isStraight(combo);
    }

    private static boolean isFourOfAKind(List<GameEngine.Card> combo) {
        return getRankCounts(combo).containsValue(4);
    }

    private static boolean isFullHouse(List<GameEngine.Card> combo) {
        Map<GameEngine.Rank, Integer> counts = getRankCounts(combo);
        return counts.containsValue(3) && counts.containsValue(2);
    }

    private static boolean isFlush(List<GameEngine.Card> combo) {
        GameEngine.Suit firstSuit = combo.get(0).suit;
        return combo.stream().allMatch(c -> c.suit == firstSuit);
    }

    private static boolean isStraight(List<GameEngine.Card> combo) {
        boolean normalStraight = true;
        for (int i = 0; i < combo.size() - 1; i++) {
            if (combo.get(i).rank.value - 1 != combo.get(i + 1).rank.value) {
                normalStraight = false;
                break;
            }
        }

        boolean lowStraight = combo.get(0).rank == GameEngine.Rank.ACE &&
                combo.get(1).rank == GameEngine.Rank.FIVE &&
                combo.get(2).rank == GameEngine.Rank.FOUR &&
                combo.get(3).rank == GameEngine.Rank.THREE &&
                combo.get(4).rank == GameEngine.Rank.TWO;

        return normalStraight || lowStraight;
    }

    private static boolean isThreeOfAKind(List<GameEngine.Card> combo) {
        return getRankCounts(combo).containsValue(3);
    }

    private static boolean isTwoPair(List<GameEngine.Card> combo) {
        Map<GameEngine.Rank, Integer> counts = getRankCounts(combo);
        return counts.values().stream().filter(v -> v == 2).count() >= 2;
    }

    private static boolean isOnePair(List<GameEngine.Card> combo) {
        return getRankCounts(combo).containsValue(2);
    }

    private static Map<GameEngine.Rank, Integer> getRankCounts(List<GameEngine.Card> combo) {
        Map<GameEngine.Rank, Integer> counts = new HashMap<>();
        for (GameEngine.Card card : combo) {
            counts.put(card.rank, counts.getOrDefault(card.rank, 0) + 1);
        }
        return counts;
    }
}