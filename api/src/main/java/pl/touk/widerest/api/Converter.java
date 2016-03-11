package pl.touk.widerest.api;

public interface Converter<Entity, Dto> {

    default Dto createDto(final Entity entity) {
        return createDto(entity, false, true);
    };

    Dto createDto(final Entity entity, final boolean embed, final boolean link);

    default Entity createEntity(final Dto dto) {
        throw new UnsupportedOperationException();
    }

    default Entity updateEntity(final Entity entity, final Dto dto) {
        throw new UnsupportedOperationException();
    }

}
