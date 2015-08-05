package pl.touk.widerest.multitenancy.sampledomain;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

@Repository
@RepositoryRestResource(path = "samples", collectionResourceRel = "samples")
public interface SampleEntityRepository extends PagingAndSortingRepository<SampleEntity, Long> {

}
