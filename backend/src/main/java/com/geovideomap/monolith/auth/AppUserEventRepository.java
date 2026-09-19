package com.geovideomap.monolith.auth;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserEventRepository extends JpaRepository<AppUserEvent, Long> {
}
