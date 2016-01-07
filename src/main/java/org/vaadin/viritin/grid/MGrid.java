package org.vaadin.viritin.grid;

import com.vaadin.data.Item;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.server.Extension;
import org.vaadin.viritin.grid.utils.GridUtils;

import com.vaadin.ui.Grid;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.vaadin.viritin.LazyList;
import static org.vaadin.viritin.LazyList.DEFAULT_PAGE_SIZE;
import org.vaadin.viritin.ListContainer;
import org.vaadin.viritin.SortableLazyList;

/**
 *
 * @param <T> the entity type listed in the Grid
 */
public class MGrid<T> extends Grid {

    public MGrid() {
    }

    /**
     * Creates a new instance of MGrid that contains certain types of rows.
     *
     * @param typeOfRows the type of entities that are listed in the grid
     */
    public MGrid(Class<T> typeOfRows) {
        setContainerDataSource(new ListContainer(typeOfRows));
    }

    /**
     * Creates a new instance of MGrid with given list of rows.
     *
     * @param listOfEntities the list of entities to be displayed in the grid
     */
    public MGrid(List<T> listOfEntities) {
        setRows(listOfEntities);
    }

    /**
     * A shorthand to create MGrid using LazyList. By default page size of
     * LazyList.DEFAULT_PAGE_SIZE (30) is used.
     *
     * @param pageProvider the interface via entities are fetched
     * @param countProvider the interface via the count of items is detected
     */
    public MGrid(LazyList.PagingProvider<T> pageProvider,
            LazyList.CountProvider countProvider) {
        this(new LazyList(pageProvider, countProvider, DEFAULT_PAGE_SIZE));
    }

    /**
     * A shorthand to create MGrid using a LazyList.
     *
     * @param pageProvider the interface via entities are fetched
     * @param countProvider the interface via the count of items is detected
     * @param pageSize the page size (aka maxResults) that is used in paging.
     */
    public MGrid(LazyList.PagingProvider<T> pageProvider,
            LazyList.CountProvider countProvider, int pageSize) {
        this(new LazyList(pageProvider, countProvider, pageSize));
    }

    /**
     * A shorthand to create an MGrid using SortableLazyList. By default page
     * size of LazyList.DEFAULT_PAGE_SIZE (30) is used.
     *
     * @param pageProvider the interface via entities are fetched
     * @param countProvider the interface via the count of items is detected
     */
    public MGrid(SortableLazyList.SortablePagingProvider<T> pageProvider,
            LazyList.CountProvider countProvider) {
        this(new SortableLazyList(pageProvider, countProvider, DEFAULT_PAGE_SIZE));
    }

    /**
     * A shorthand to create MTable using SortableLazyList.
     *
     * @param pageProvider the interface via entities are fetched
     * @param countProvider the interface via the count of items is detected
     * @param pageSize the page size (aka maxResults) that is used in paging.
     */
    public MGrid(SortableLazyList.SortablePagingProvider<T> pageProvider,
            LazyList.CountProvider countProvider, int pageSize) {
        this(new SortableLazyList(pageProvider, countProvider, pageSize));
    }

    /**
     * Enables saving/loading grid settings (visible columns, sort order, etc)
     * to cookies.
     *
     * @param settingsName cookie name where settings are saved should be
     * unique.
     */
    public void attachSaveSettings(String settingsName) {
        GridUtils.attachToGrid(this, settingsName);
    }

    public MGrid<T> setRows(List<T> rows) {
        setContainerDataSource(new ListContainer(rows));
        return this;
    }

    public MGrid<T> setRows(T... rows) {
        setContainerDataSource(new ListContainer(Arrays.asList(rows)));
        return this;
    }

    @Override
    public T getSelectedRow() throws IllegalStateException {
        return (T) super.getSelectedRow();
    }

    /**
     *
     * @param entity the entity (row) to be selected.
     * @return <code>true</code> if the selection state changed,
     * <code>false</code> if the itemId already was selected
     */
    public boolean selectRow(T entity) {
        return select(entity);
    }

    /**
     * @deprecated use the typed selectRow instead
     */
    @Deprecated
    @Override
    public boolean select(Object itemId) throws IllegalArgumentException, IllegalStateException {
        return super.select(itemId);
    }

    public Collection<T> getSelectedRowsWithType() {
        // Maybe this is more complicated than it should be :-)
        return (Collection<T>) super.getSelectedRows();
    }

    public MGrid<T> withProperties(String... propertyIds) {
        setColumns((Object[]) propertyIds);
        return this;
    }

    private FieldGroup.CommitHandler reloadDataEfficientlyAfterEditor;

    @Override
    public void setEditorEnabled(boolean isEnabled) throws IllegalStateException {
        super.setEditorEnabled(isEnabled);
        ensureRowRefreshListener(isEnabled);

    }

    protected void ensureRowRefreshListener(boolean isEnabled) {
        if (isEnabled && reloadDataEfficientlyAfterEditor == null) {
            reloadDataEfficientlyAfterEditor = new FieldGroup.CommitHandler() {
                @Override
                public void preCommit(FieldGroup.CommitEvent commitEvent) throws FieldGroup.CommitException {
                }

                @Override
                public void postCommit(FieldGroup.CommitEvent commitEvent) throws FieldGroup.CommitException {
                    Item itemDataSource = commitEvent.getFieldBinder().
                            getItemDataSource();
                    if (itemDataSource instanceof ListContainer.DynaBeanItem) {
                        ListContainer.DynaBeanItem dynaBeanItem = (ListContainer.DynaBeanItem) itemDataSource;
                        T bean = (T) dynaBeanItem.getBean();
                        refreshRow(bean);
                    }
                }

            };
            getEditorFieldGroup().addCommitHandler(
                    reloadDataEfficientlyAfterEditor);
        }
    }

    /**
     * Manually forces refresh of the row that represents given entity.
     * ListContainer backing MGrid/MTable don't support property change
     * listeners (to save memory and CPU cycles). In some case with Grid, if you
     * know only certain row(s) are changed, you can make a smaller client side
     * change by refreshing rows with this method, instead of refreshing the 
     * whole Grid (e.g. by re-assigning the bean list).
     * <p>
     * This method is automatically called if you use "editor row".
     *
     * @param bean the bean whose row should be refreshed.
     */
    public void refreshRow(T bean) {
        Collection<Extension> extensions = getExtensions();
        for (Extension extension : extensions) {
            // Calling with reflection for 7.6-7.5 compatibility
            if(extension.getClass().getName().contains("RpcDataProviderExtension")) {
                try {
                    Method method = extension.getClass().getMethod("updateRowData", Object.class);
                    method.invoke(extension, bean);
                    break;
                } catch (NoSuchMethodException ex) {
                    Logger.getLogger(MGrid.class.getName()).
                            log(Level.SEVERE, null, ex);
                } catch (SecurityException ex) {
                    Logger.getLogger(MGrid.class.getName()).
                            log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(MGrid.class.getName()).
                            log(Level.SEVERE, null, ex);
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(MGrid.class.getName()).
                            log(Level.SEVERE, null, ex);
                } catch (InvocationTargetException ex) {
                    Logger.getLogger(MGrid.class.getName()).
                            log(Level.SEVERE, null, ex);
                }
            }
        }
    }

}
