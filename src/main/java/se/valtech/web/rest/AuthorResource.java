package se.valtech.web.rest;

import com.codahale.metrics.annotation.Timed;
import se.valtech.domain.Author;
import se.valtech.repository.AuthorRepository;
import se.valtech.repository.search.AuthorSearchRepository;
import se.valtech.web.rest.util.HeaderUtil;
import se.valtech.web.rest.util.PaginationUtil;
import se.valtech.web.rest.dto.AuthorDTO;
import se.valtech.web.rest.mapper.AuthorMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * REST controller for managing Author.
 */
@RestController
@RequestMapping("/api")
public class AuthorResource {

    private final Logger log = LoggerFactory.getLogger(AuthorResource.class);
        
    @Inject
    private AuthorRepository authorRepository;
    
    @Inject
    private AuthorMapper authorMapper;
    
    @Inject
    private AuthorSearchRepository authorSearchRepository;
    
    /**
     * POST  /authors -> Create a new author.
     */
    @RequestMapping(value = "/authors",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<AuthorDTO> createAuthor(@RequestBody AuthorDTO authorDTO) throws URISyntaxException {
        log.debug("REST request to save Author : {}", authorDTO);
        if (authorDTO.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("author", "idexists", "A new author cannot already have an ID")).body(null);
        }
        Author author = authorMapper.authorDTOToAuthor(authorDTO);
        author = authorRepository.save(author);
        AuthorDTO result = authorMapper.authorToAuthorDTO(author);
        authorSearchRepository.save(author);
        return ResponseEntity.created(new URI("/api/authors/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("author", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /authors -> Updates an existing author.
     */
    @RequestMapping(value = "/authors",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<AuthorDTO> updateAuthor(@RequestBody AuthorDTO authorDTO) throws URISyntaxException {
        log.debug("REST request to update Author : {}", authorDTO);
        if (authorDTO.getId() == null) {
            return createAuthor(authorDTO);
        }
        Author author = authorMapper.authorDTOToAuthor(authorDTO);
        author = authorRepository.save(author);
        AuthorDTO result = authorMapper.authorToAuthorDTO(author);
        authorSearchRepository.save(author);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("author", authorDTO.getId().toString()))
            .body(result);
    }

    /**
     * GET  /authors -> get all the authors.
     */
    @RequestMapping(value = "/authors",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Transactional(readOnly = true)
    public ResponseEntity<List<AuthorDTO>> getAllAuthors(Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Authors");
        Page<Author> page = authorRepository.findAll(pageable); 
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/authors");
        return new ResponseEntity<>(page.getContent().stream()
            .map(authorMapper::authorToAuthorDTO)
            .collect(Collectors.toCollection(LinkedList::new)), headers, HttpStatus.OK);
    }

    /**
     * GET  /authors/:id -> get the "id" author.
     */
    @RequestMapping(value = "/authors/{id}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<AuthorDTO> getAuthor(@PathVariable Long id) {
        log.debug("REST request to get Author : {}", id);
        Author author = authorRepository.findOne(id);
        AuthorDTO authorDTO = authorMapper.authorToAuthorDTO(author);
        return Optional.ofNullable(authorDTO)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /authors/:id -> delete the "id" author.
     */
    @RequestMapping(value = "/authors/{id}",
        method = RequestMethod.DELETE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> deleteAuthor(@PathVariable Long id) {
        log.debug("REST request to delete Author : {}", id);
        authorRepository.delete(id);
        authorSearchRepository.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("author", id.toString())).build();
    }

    /**
     * SEARCH  /_search/authors/:query -> search for the author corresponding
     * to the query.
     */
    @RequestMapping(value = "/_search/authors/{query}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public List<AuthorDTO> searchAuthors(@PathVariable String query) {
        log.debug("REST request to search Authors for query {}", query);
        return StreamSupport
            .stream(authorSearchRepository.search(queryStringQuery(query)).spliterator(), false)
            .map(authorMapper::authorToAuthorDTO)
            .collect(Collectors.toList());
    }
}
