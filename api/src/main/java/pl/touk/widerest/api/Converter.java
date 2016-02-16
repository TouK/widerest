package pl.touk.widerest.api;

public interface Converter<Entity, Dto> {

    Dto createDto(Entity entity, boolean embed);

    Entity createEntity(Dto dto);

    Entity updateEntity(Entity entity, Dto dto);

}
