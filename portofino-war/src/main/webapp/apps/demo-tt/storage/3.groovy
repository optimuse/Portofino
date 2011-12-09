import com.manydesigns.portofino.actions.CrudAction
import com.manydesigns.portofino.model.pages.AccessLevel
import com.manydesigns.portofino.system.model.users.annotations.RequiresPermissions
import com.manydesigns.portofino.system.model.users.annotations.SupportsPermissions

@SupportsPermissions([ CrudAction.PERMISSION_CREATE, CrudAction.PERMISSION_EDIT, CrudAction.PERMISSION_DELETE ])
@RequiresPermissions(level = AccessLevel.VIEW)
class _3 extends CrudAction {

    void createSetup(object) {
        object.project_id = project.id;
        object.lock_version = 0;
        object.done_ratio = 0;
        object.author_id = 1;
    }

    boolean createValidate(object) {
        Date now = new Date();
        object.created_on = now;
        object.updated_on = now;
        return true;
    }

    boolean editValidate(object) {
        object.updated_on = new Date();
        return true;
    }
}