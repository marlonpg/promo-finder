package dev.gamba.promofinder.monitor;

/** Published when a watch's check state changes, so open pages can refresh it. */
public record WatchUpdated(Long watchId) {
}
