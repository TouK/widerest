package pl.touk.widerest.api;

public interface Converter<Entity, Dto> {

    Dto createDto(final Entity entity, final boolean embed);

    default Entity createEntity(final Dto dto) {
        throw new UnsupportedOperationException();
    }

    default Entity updateEntity(final Entity entity, final Dto dto) {
        throw new UnsupportedOperationException();
    }

}
