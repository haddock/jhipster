package se.valtech.web.rest.mapper;

import se.valtech.domain.*;
import se.valtech.web.rest.dto.BookDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity Book and its DTO BookDTO.
 */
@Mapper(componentModel = "spring", uses = {})
public interface BookMapper {

    @Mapping(source = "author.id", target = "authorId")
    @Mapping(source = "author.name", target = "authorName")
    BookDTO bookToBookDTO(Book book);

    @Mapping(source = "authorId", target = "author")
    Book bookDTOToBook(BookDTO bookDTO);

    default Author authorFromId(Long id) {
        if (id == null) {
            return null;
        }
        Author author = new Author();
        author.setId(id);
        return author;
    }
}
