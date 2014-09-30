package cz.cvut.kbss.ontodriver.sesame.connector;

import java.util.Collection;
import java.util.List;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;

import cz.cvut.kbss.ontodriver.Closeable;
import cz.cvut.kbss.ontodriver.sesame.exceptions.SesameDriverException;

public interface Connector extends Closeable, StatementExecutor {

	/**
	 * Explicitly starts a transaction.
	 * 
	 * @throws SesameDriverException
	 *             If unable to start transaction
	 */
	public void begin() throws SesameDriverException;

	/**
	 * Commits the changes made since transaction beginning. </p>
	 * 
	 * @throws SesameDriverException
	 *             If an error occurs during commit
	 * @see #begin()
	 */
	public void commit() throws SesameDriverException;

	/**
	 * Rolls back changes made since transaction beginning. </p>
	 * 
	 * @throws SesameDriverException
	 *             If an error occurs when rolling back
	 * @see #begin()
	 */
	public void rollback() throws SesameDriverException;

	/**
	 * Gets resources representing currently existing contexts in the
	 * repository.
	 * 
	 * @return List of resources
	 * @throws SesameDriverException
	 *             If repository access error occurs
	 */
	public List<Resource> getContexts() throws SesameDriverException;

	/**
	 * Gets Sesame value factory.
	 * 
	 * @return {@link ValueFactory}
	 */
	public ValueFactory getValueFactory();

	/**
	 * Finds statements corresponding to the specified criteria. </p>
	 * 
	 * Note that some of the parameters are optional
	 * 
	 * @param subject
	 *            Statement subject, optional
	 * @param property
	 *            Statement property, optional
	 * @param value
	 *            Statement value, optional
	 * @param includeInferred
	 *            Whether to include inferred statements as well
	 * @param contexts
	 *            Optionally specify contexts in which the search should be
	 *            performed. If not specified, the default one is used
	 * @return Collection of matching statements
	 * @throws SesameDriverException
	 *             If a repository access error occurs
	 */
	public Collection<Statement> findStatements(Resource subject, URI property, Value value,
			boolean includeInferred, URI... contexts) throws SesameDriverException;

	/**
	 * Adds the specified statements to the underlying repository. </p>
	 * 
	 * Note that this operation is transactional and the changes are required to
	 * be persistent only after successful {@link #commit()}.
	 * 
	 * @param statements
	 *            The statements to add
	 * @throws IllegalStateException
	 *             If transaction is not active
	 * @throws SesameDriverException
	 *             If a repository access error occurs
	 */
	public void addStatements(Collection<Statement> statements) throws SesameDriverException;

	/**
	 * Removes the specified statements from the underlying repository. </p>
	 * 
	 * Note that this operation is transactional and the changes are required to
	 * be persistent only after successful {@link #commit()}.
	 * 
	 * @param statements
	 *            The statements to remove
	 * @throws IllegalStateException
	 *             If transaction is not active
	 * @throws SesameDriverException
	 *             If a repository access error occurs
	 */
	public void removeStatements(Collection<Statement> statements) throws SesameDriverException;
}