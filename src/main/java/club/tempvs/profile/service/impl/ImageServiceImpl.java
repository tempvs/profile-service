package club.tempvs.profile.service.impl;

import club.tempvs.profile.dto.ImageDto;
import club.tempvs.profile.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    @Override
    public void store(ImageDto payload) {
        // Image persistence is intentionally deferred until the replacement provider is integrated.
/*        imageEventProcessor.replace()
                .send(withPayload(payload).build());*/
    }

    @Override
    public void delete(String belongsTo, Long entityId) {
        String query = String.format("%1$s::%2$d", belongsTo, entityId);
        // Keep delete semantics stable for callers; actual provider-specific cleanup will come later.
/*        imageEventProcessor.deleteForEntity()
                .send(withPayload(query).build());*/
    }
}
