package dev.hbn.doodle.modules.timeslot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TimeslotRepository extends JpaRepository<TimeslotEntity, Long>, JpaSpecificationExecutor<TimeslotEntity> {

}
