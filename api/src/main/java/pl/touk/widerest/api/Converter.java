package pl.touk.widerest.api;

public interface Converter<Entity, Dto> {

    Dto createDto(final Entity entity, final boolean embed);

    Entity createEntity(final Dto dto);

    Entity updateEntity(final Entity entity, final Dto dto);

}
