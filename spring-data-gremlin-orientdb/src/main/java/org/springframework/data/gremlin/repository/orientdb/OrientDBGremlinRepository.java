package org.springframework.data.gremlin.repository.orientdb;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.gremlin.repository.GremlinGraphAdapter;
import org.springframework.data.gremlin.repository.SimpleGremlinRepository;
import org.springframework.data.gremlin.schema.GremlinSchema;
import org.springframework.data.gremlin.tx.GremlinGraphFactory;
import org.springframework.data.gremlin.tx.orientdb.OrientDBGremlinGraphFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * OrientDB specific extension of the {@link SimpleGremlinRepository} providing custom implementations of {@code count()}, {@code deleteAll()},
 * {@code findAll(Pageable)} and {@code findAll()}.
 *
 * @author Gman
 */
public class OrientDBGremlinRepository<T> extends SimpleGremlinRepository<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrientDBGremlinRepository.class);

    OrientDBGremlinGraphFactory orientGraphFactory;

    public OrientDBGremlinRepository(GremlinGraphFactory dbf, GremlinGraphAdapter graphAdapter, GremlinSchema<T> mapper) {
        super(dbf, graphAdapter, mapper);
        this.orientGraphFactory = (OrientDBGremlinGraphFactory) dbf;
    }

    @Override
    @Transactional
    public long count() {
        long count = 0;
        try {
            if (schema.isVertexSchema()) {
                count = orientGraphFactory.graph().countVertices(schema.getClassName());
            } else {
                count = orientGraphFactory.graph().countEdges(schema.getClassName());
            }
        } catch (Exception e) {
        }
        return count;
    }

    @Transactional
    @Override
    public void deleteAll() {
        OrientGraph graph = orientGraphFactory.graph();
        for (Element vertex : findAllElementsForSchema()) {
            vertex.remove();
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Page<T> findAll(Pageable pageable) {

        List<T> result = new ArrayList<T>();
        int total = 0;
        int prevOffset = pageable.getOffset();
        int offset = pageable.getOffset() + pageable.getPageSize();
        for (Element element : findAllElementsForSchema()) {
            if (total >= prevOffset && total < offset) {
                result.add(schema.loadFromGraph(element));
            }
            total++;
        }
        return new PageImpl<T>(result, pageable, total);

    }

    @Transactional(readOnly = true)
    @Override
    public Iterable<T> findAll() {

        List<T> result = new ArrayList<T>();
        for (Element edge : findAllElementsForSchema()) {
            result.add(schema.loadFromGraph(edge));
        }
        return result;

    }

    public Iterable<Element> findAllElementsForSchema() {

        if (schema.isVertexSchema()) {
            return findALlVerticiesForSchema();
        } else if (schema.isEdgeSchema()) {
            return findAllEdgesForSchema();
        } else {
            throw new IllegalStateException("GremlinSchema is neither VERTEX or EDGE!!");
        }
    }

    public Iterable<Element> findALlVerticiesForSchema() {

        OrientGraph graph = orientGraphFactory.graph();
        List<Element> result = new ArrayList<>();
        for (Vertex vertex : graph.getVerticesOfClass(schema.getClassName())) {
            result.add(vertex);
        }
        return result;
    }

    public Iterable<Element> findAllEdgesForSchema() {

        OrientGraph graph = orientGraphFactory.graph();
        List<Element> result = new ArrayList<>();
        for (Edge edge : graph.getEdgesOfClass(schema.getClassName())) {
            result.add(edge);
        }
        return result;
    }

}
