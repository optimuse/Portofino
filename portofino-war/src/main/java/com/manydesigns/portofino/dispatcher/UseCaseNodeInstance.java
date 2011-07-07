/*
 * Copyright (C) 2005-2010 ManyDesigns srl.  All rights reserved.
 * http://www.manydesigns.com/
 *
 * Unless you have purchased a commercial license agreement from ManyDesigns srl,
 * the following license terms apply:
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * There are special exceptions to the terms and conditions of the GPL
 * as it is applied to this software. View the full text of the
 * exception in file OPEN-SOURCE-LICENSE.txt in the directory of this
 * software distribution.
 *
 * This program is distributed WITHOUT ANY WARRANTY; and without the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see http://www.gnu.org/licenses/gpl.txt
 * or write to:
 * Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307  USA
 *
 */

package com.manydesigns.portofino.dispatcher;

import com.manydesigns.elements.reflection.ClassAccessor;
import com.manydesigns.portofino.context.Context;
import com.manydesigns.portofino.model.datamodel.Table;
import com.manydesigns.portofino.model.site.UseCaseNode;
import com.manydesigns.portofino.model.site.usecases.UseCase;
import com.manydesigns.portofino.util.PkHelper;

import java.io.Serializable;

/*
* @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
* @author Angelo Lupo          - angelo.lupo@manydesigns.com
* @author Giampiero Granatella - giampiero.granatella@manydesigns.com
* @author Alessio Stalla - alessio.stalla@manydesigns.com
*/
public class UseCaseNodeInstance extends SiteNodeInstance {

    protected final String pk;
    protected final UseCase useCase;

    protected final ClassAccessor classAccessor;
    protected final Table baseTable;
    protected final PkHelper pkHelper;

    protected Object object;

    public UseCaseNodeInstance(Context context, UseCaseNode siteNode, String mode, String param) {
        super(context, siteNode, mode);
        this.pk = param;
        this.useCase = siteNode.getUseCase();
        classAccessor = context.getUseCaseAccessor(useCase);
        baseTable = useCase.getActualTable();
        pkHelper = new PkHelper(classAccessor);
        if(UseCaseNode.MODE_DETAIL.equals(mode)) {
            loadObject();
        }
    }

    private void loadObject() {
        loadObject(pk);
    }

    private void loadObject(String pk) {
        Serializable pkObject = pkHelper.parsePkString(pk);
        object = context.getObjectByPk(baseTable.getQualifiedName(), pkObject);
    }

    // Getter/setter

    public String getPk() {
        return pk;
    }

    @Override
    public UseCaseNode getSiteNode() {
        return (UseCaseNode) super.getSiteNode();
    }

    public UseCase getUseCase() {
        return useCase;
    }

    public ClassAccessor getClassAccessor() {
        return classAccessor;
    }

    public Table getBaseTable() {
        return baseTable;
    }

    public PkHelper getPkHelper() {
        return pkHelper;
    }

    public Object getObject() {
        return object;
    }
}