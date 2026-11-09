package dev.hbn.doodle.modules.meeting;

import dev.hbn.doodle.modules.timeslot.TimeslotEntity;
import dev.hbn.doodle.modules.timeslot.TimeslotRepository;
import dev.hbn.doodle.modules.timeslot.TimeslotStatus;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MeetingService implements MeetingContract {

    private final MeetingRepository meetingRepository;
    private final TimeslotRepository timeslotRepository;

    public MeetingService(MeetingRepository meetingRepository, TimeslotRepository timeslotRepository) {
        this.meetingRepository = meetingRepository;
        this.timeslotRepository = timeslotRepository;
    }

    @Override
    public MeetingPortable get(long id) {
        var found = meetingRepository.findById(id);
        if (found.isPresent()) {
            return MeetingPortable.create(found.get());
        } else {
            throw new EntityNotFoundException("Meeting with id " + id + " not found");
        }
    }

    @Override
    @Transactional
    public MeetingPortable create(MeetingCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request");
        }
        if (!request.isValid()) {
            throw new IllegalArgumentException("request");
        }
        Optional<TimeslotEntity> timeslot = timeslotRepository.findById(request.timeslotId());
        if (timeslot.isEmpty() || timeslot.get().getStatus() == TimeslotStatus.BUSY) {
            throw new IllegalArgumentException("timeslot not found or is scheduled");
        }

        MeetingEntity entity = new MeetingEntity();
        entity.setTimeslot(timeslot.get());
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setParticipants(request.participants());
        meetingRepository.save(entity);

        timeslot.get().setStatus(TimeslotStatus.BUSY);
        timeslotRepository.save(timeslot.get());

        return MeetingPortable.create(entity);
    }

    @Transactional
    @Override
    public void delete(long id) {
        Optional<MeetingEntity> possibleMeeting = meetingRepository.findById(id);
        if (possibleMeeting.isEmpty()) {
            throw new EntityNotFoundException("Meeting with id " + id + " not found");
        }
        var meeting = possibleMeeting.get();
        var timeslot = meeting.getTimeslot();
        timeslot.setStatus(TimeslotStatus.FREE);
        timeslotRepository.save(timeslot);
        meetingRepository.delete(meeting);
    }

    @Override
    public Page<MeetingPortable> paged(Pageable pageable) {
        return meetingRepository.findAll(pageable)
                .map(MeetingPortable::create);
    }
}
