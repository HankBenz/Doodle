package com.doodle.scheduling.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "doodle")
public record DoodleProperties(Slot slot, Availability availability, Meeting meeting) {

    public record Slot(int minDurationMinutes, int maxDurationMinutes) {
        public Slot {
            if (minDurationMinutes < 1 || maxDurationMinutes < minDurationMinutes) {
                throw new IllegalArgumentException("Slot duration bounds are invalid.");
            }
        }
    }

    public record Availability(int maxWindowDays, int maxUsers) {
        public Availability {
            if (maxWindowDays < 1 || maxUsers < 1) {
                throw new IllegalArgumentException("Availability bounds are invalid.");
            }
        }
    }

    public record Meeting(int maxParticipants) {
        public Meeting {
            if (maxParticipants < 1) {
                throw new IllegalArgumentException("Meeting participant cap is invalid.");
            }
        }
    }
}
