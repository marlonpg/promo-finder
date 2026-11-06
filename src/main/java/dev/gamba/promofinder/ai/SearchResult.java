package dev.gamba.promofinder.ai;

import java.util.List;

public record SearchResult(List<FoundOffer> offers, String notes, Double costUsd) {
}
