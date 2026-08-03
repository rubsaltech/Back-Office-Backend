package com.backoffice.pos.floor.dto;

import com.backoffice.pos.floor.Floor;
import com.backoffice.pos.floor.RestaurantTable;
import com.backoffice.pos.floor.TableStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/** Grouped DTOs for the floor-plan module. */
public final class FloorDtos {

    private FloorDtos() {
    }

    public record FloorRequest(@NotBlank String name) {
    }

    public record FloorResponse(Long id, String name, long tableCount) {
        public static FloorResponse from(Floor f, long tableCount) {
            return new FloorResponse(f.getId(), f.getName(), tableCount);
        }
    }

    public record TableRequest(
            @NotBlank String name,
            Long floorId,
            @PositiveOrZero int seats,
            TableStatus status
    ) {
    }

    public record TableResponse(Long id, String name, Long floorId, String floorName, int seats, TableStatus status) {
        public static TableResponse from(RestaurantTable t) {
            return new TableResponse(
                    t.getId(), t.getName(),
                    t.getFloor() != null ? t.getFloor().getId() : null,
                    t.getFloor() != null ? t.getFloor().getName() : null,
                    t.getSeats(), t.getStatus());
        }
    }
}
