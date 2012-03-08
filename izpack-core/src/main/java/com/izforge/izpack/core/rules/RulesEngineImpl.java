/*
 * IzPack - Copyright 2001-2008 Julien Ponge, All Rights Reserved.
 *
 * http://izpack.org/
 * http://izpack.codehaus.org/
 *
 * Copyright 2007 Dennis Reil
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.izforge.izpack.core.rules;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import com.izforge.izpack.api.adaptator.IXMLElement;
import com.izforge.izpack.api.adaptator.XMLException;
import com.izforge.izpack.api.adaptator.impl.XMLElementImpl;
import com.izforge.izpack.api.adaptator.impl.XMLWriter;
import com.izforge.izpack.api.container.BindeableContainer;
import com.izforge.izpack.api.data.AutomatedInstallData;
import com.izforge.izpack.api.data.Pack;
import com.izforge.izpack.api.exception.IzPackException;
import com.izforge.izpack.api.rules.Condition;
import com.izforge.izpack.api.rules.ConditionReference;
import com.izforge.izpack.api.rules.ConditionWithMultipleOperands;
import com.izforge.izpack.api.rules.RulesEngine;
import com.izforge.izpack.core.container.ConditionContainer;
import com.izforge.izpack.core.rules.logic.AndCondition;
import com.izforge.izpack.core.rules.logic.NotCondition;
import com.izforge.izpack.core.rules.logic.OrCondition;
import com.izforge.izpack.core.rules.logic.XorCondition;
import com.izforge.izpack.core.rules.process.JavaCondition;
import com.izforge.izpack.core.rules.process.PackselectionCondition;
import com.izforge.izpack.merge.resolve.ClassPathCrawler;


/**
 * The rules engine class is the central point for checking conditions
 *
 * @author Dennis Reil, <Dennis.Reil@reddot.de> created: 09.11.2006, 13:48:39
 */
public class RulesEngineImpl implements RulesEngine
{
    private static final Logger logger = Logger.getLogger(RulesEngineImpl.class.getName());

    private static final long serialVersionUID = 3966346766966632860L;

    protected Map<String, String> panelconditions;

    protected Map<String, String> packconditions;

    protected Map<String, String> optionalpackconditions;

    protected Map<String, Condition> conditionsmap = new HashMap<String, Condition>();
    private Set<ConditionReference> refConditions = new HashSet<ConditionReference>();

    protected AutomatedInstallData installdata;
    private ClassPathCrawler classPathCrawler;
    private BindeableContainer container;

    public RulesEngineImpl(AutomatedInstallData installdata, ClassPathCrawler classPathCrawler, ConditionContainer container)
    {
        this.installdata = installdata;
        this.classPathCrawler = classPathCrawler;
        this.container = container;
        conditionsmap = new HashMap<String, Condition>();
        this.panelconditions = new HashMap<String, String>();
        this.packconditions = new HashMap<String, String>();
        this.optionalpackconditions = new HashMap<String, String>();
        initStandardConditions();
    }

    public RulesEngineImpl(ClassPathCrawler classPathCrawler, ConditionContainer container)
    {
        this(null, classPathCrawler, container);
    }

    /**
     * initializes built-in conditions like os conditions and package conditions
     */
    private void initStandardConditions()
    {
        logger.fine("Initializing built-in conditions");
        initOsConditions();
        if ((installdata != null) && (installdata.getAllPacks() != null))
        {
            logger.fine("Initializing built-in conditions for packs");
            for (Pack pack : installdata.getAllPacks())
            {
                if (pack.id != null)
                {
                    // automatically add packselection condition
                    PackselectionCondition packselcond = new PackselectionCondition();
                    packselcond.setInstalldata(installdata);
                    packselcond.setId("izpack.selected." + pack.id);
                    packselcond.setPackid(pack.id);
                    conditionsmap.put(packselcond.getId(), packselcond);

                    String condition = pack.getCondition();
                    logger.fine("Checking pack condition \"" + condition + "\" for pack \""
                            + pack.id + "\"");
                    if ((condition != null) && !condition.isEmpty())
                    {
                        logger.fine("Adding pack condition \"" + condition + "\" for pack \""
                                + pack.id + "\"");
                        packconditions.put(pack.id, condition);
                    }
                }
            }
        }
    }

    private void initOsConditions()
    {
        createBuiltinOsCondition("IS_AIX", "izpack.aixinstall");
        createBuiltinOsCondition("IS_WINDOWS", "izpack.windowsinstall");
        createBuiltinOsCondition("IS_WINDOWS_XP", "izpack.windowsinstall.xp");
        createBuiltinOsCondition("IS_WINDOWS_2003", "izpack.windowsinstall.2003");
        createBuiltinOsCondition("IS_WINDOWS_VISTA", "izpack.windowsinstall.vista");
        createBuiltinOsCondition("IS_WINDOWS_7", "izpack.windowsinstall.7");
        createBuiltinOsCondition("IS_LINUX", "izpack.linuxinstall");
        createBuiltinOsCondition("IS_SUNOS", "izpack.solarisinstall");
        createBuiltinOsCondition("IS_MAC", "izpack.macinstall");
        createBuiltinOsCondition("IS_SUNOS", "izpack.solarisinstall");
        createBuiltinOsCondition("IS_SUNOS_X86", "izpack.solarisinstall.x86");
        createBuiltinOsCondition("IS_SUNOS_SPARC", "izpack.solarisinstall.sparc");
    }

    private void createBuiltinOsCondition(String osVersionField, String conditionId)
    {
        JavaCondition condition = new JavaCondition("com.izforge.izpack.util.OsVersion", osVersionField, true, "true", "boolean");
        condition.setInstalldata(installdata);
        condition.setId(conditionId);
        conditionsmap.put(condition.getId(), condition);
    }

    @Override
    public void readConditionMap(Map<String, Condition> rules)
    {
        conditionsmap.putAll(rules);
        for (String key : rules.keySet())
        {
            Condition condition = rules.get(key);
            condition.setInstalldata(installdata);
        }
    }

    /**
     * Returns the current known condition ids.
     *
     * @return
     */
    @Override
    public Set<String> getKnownConditionIds()
    {
        return conditionsmap.keySet();
    }

    @Override
    public Condition instanciateCondition(IXMLElement condition)
    {
        String condid = condition.getAttribute("id");
        String condtype = condition.getAttribute("type");
        Condition result = null;
        if (condtype != null)
        {
            String conditionclassname = "";
            if (condtype.indexOf('.') > -1)
            {
                conditionclassname = condtype;
            }
            else
            {
                String conditiontype = condtype.toLowerCase();
                conditionclassname = conditiontype.substring(0, 1).toUpperCase()
                        + conditiontype.substring(1, conditiontype.length());
                conditionclassname += "Condition";
            }
            try
            {
                Class<Condition> conditionclass = classPathCrawler.findClass(conditionclassname);
                if (condid == null || condid.isEmpty() || "UNKNOWN".equals(condid))
                {
                    condid = conditionclassname + "-"
                             + UUID.randomUUID().toString();
                    logger.fine("Random condition id " + condid + " generated");
                }
                container.addComponent(condid, conditionclass);
                result = (Condition) container.getComponent(condid);
                result.setId(condid);
                result.setInstalldata(installdata);
                result.readFromXML(condition);
                conditionsmap.put(condid, result);
                if (result instanceof ConditionReference)
                {
                    refConditions.add((ConditionReference)result);
                }
            }
            catch (Exception e)
            {
                throw new IzPackException(e);
            }
        }
        return result;
    }

    @Override
    public void resolveConditions() throws Exception {
        for (ConditionReference refCondition : refConditions)
        {
            refCondition.resolveReference();
        }
    }

    /**
     * Read the spec for the conditions
     *
     * @param conditionsspec
     */
    @Override
    public void analyzeXml(IXMLElement conditionsspec)
    {
        if (conditionsspec == null)
        {
            logger.fine("No conditions specification found");
            return;
        }
        if (conditionsspec.hasChildren())
        {
            // read in the condition specs
            List<IXMLElement> childs = conditionsspec.getChildrenNamed("condition");

            for (IXMLElement condition : childs)
            {
                Condition cond = instanciateCondition(condition);
                if (cond != null)
                {
                    // this.conditionslist.add(cond);
                    String condid = cond.getId();
                    cond.setInstalldata(installdata);
                    if ((condid != null) && !("UNKNOWN".equals(condid)))
                    {
                        conditionsmap.put(condid, cond);
                    }
                }
            }

            List<IXMLElement> panelconditionels = conditionsspec
                    .getChildrenNamed("panelcondition");
            for (IXMLElement panelel : panelconditionels)
            {
                String panelid = panelel.getAttribute("panelid");
                String conditionid = panelel.getAttribute("conditionid");
                this.panelconditions.put(panelid, conditionid);
            }

            List<IXMLElement> packconditionels = conditionsspec
                    .getChildrenNamed("packcondition");
            for (IXMLElement panelel : packconditionels)
            {
                String panelid = panelel.getAttribute("packid");
                String conditionid = panelel.getAttribute("conditionid");
                this.packconditions.put(panelid, conditionid);
                // optional install allowed, if condition is not met?
                String optional = panelel.getAttribute("optional");
                if (optional != null)
                {
                    boolean optionalinstall = Boolean.valueOf(optional);
                    if (optionalinstall)
                    {
                        // optional installation is allowed
                        this.optionalpackconditions.put(panelid, conditionid);
                    }
                }
            }
        }
    }


    /**
     * Gets the condition for the requested id.
     * The id may be one of the following:
     * A condition ID as defined in the install.xml
     * A simple expression with !,+,|,\
     * A complex expression with !,&&,||,\\ - must begin with char @
     *
     * @param id
     * @return
     */
    @Override
    public Condition getCondition(String id)
    {
        Condition result = conditionsmap.get(id);
        if (result == null)
        {
            if (id.startsWith("@"))
            {
                result = parseComplexCondition(id.substring(1));
            }
            else
            {
                result = getConditionByExpr(new StringBuffer(id));
            }
        }
        return result;
    }

    /**
     * Parses the given complex expression into a condition.
     * Understands the boolean operations && (AND), || (OR)
     * and ! (NOT).
     * <p/>
     * Precedence is:
     * NOT is evaluated first.
     * AND is evaluated after NOT, but before OR.
     * OR is evaluated last.
     * <p/>
     * Parentheses may be added at a later time.
     *
     * @param expression
     * @return
     */
    protected Condition parseComplexCondition(String expression)
    {
        Condition result = null;

        if (expression.contains("||"))
        {
            result = parseComplexOrCondition(expression);
        }
        else if (expression.contains("&&"))
        {
            result = parseComplexAndCondition(expression);
        }
        else if (expression.contains("^"))
        {
            result = parseComplexXorCondition(expression);
        }
        else if (expression.contains("!"))
        {
            result = parseComplexNotCondition(expression);
        }
        else
        {
            result = conditionsmap.get(expression);
        }

        result.setInstalldata(installdata);

        return result;
    }

    /**
     * Creates an OR condition from the given complex expression.
     * Uses the substring up to the first || delimiter as first operand and
     * the rest as second operand.
     *
     * @param expression
     * @return OrCondition
     */
    private Condition parseComplexOrCondition(String expression)
    {
        String[] parts = expression.split("\\|\\|", 2);
        OrCondition orCondition = new OrCondition(this);
        orCondition.addOperands(parseComplexCondition(parts[0].trim()), parseComplexCondition(parts[1].trim()));

        return orCondition;
    }

    /**
     * Creates a XOR condition from the given complex expression
     *
     * @param expression
     * @return
     */
    private Condition parseComplexXorCondition(String expression)
    {
        String[] parts = expression.split("\\^", 2);
        XorCondition xorCondition = new XorCondition(this);
        xorCondition.addOperands(parseComplexCondition(parts[0].trim()), parseComplexCondition(parts[1].trim()));

        return xorCondition;
    }

    /**
     * Creates an AND condition from the given complex expression.
     * Uses the expression up to the first && delimiter as first operand and
     * the rest as second operand.
     *
     * @param expression
     * @return AndCondition
     */
    private Condition parseComplexAndCondition(String expression)
    {
        String[] parts = expression.split("\\&\\&", 2);
        AndCondition andCondition = new AndCondition(this);
        andCondition.addOperands(parseComplexCondition(parts[0].trim()), parseComplexCondition(parts[1].trim()));

        return andCondition;
    }

    /**
     * Creates a NOT condition from the given complex expression.
     * Negates the result of the whole expression!
     *
     * @param expression
     * @return NotCondtion
     */
    private Condition parseComplexNotCondition(String expression)
    {
        Condition result = null;
        result = NotCondition.createFromCondition(
                parseComplexCondition(expression.substring(1).trim()),
                this);
        return result;
    }

    protected Condition getConditionByExpr(StringBuffer conditionexpr)
    {
        Condition result = null;
        int index = 0;
        while (index < conditionexpr.length())
        {
            char currentchar = conditionexpr.charAt(index);
            switch (currentchar)
            {
                case '+':
                    // and-condition
                    Condition op1 = conditionsmap.get(conditionexpr.substring(0, index));
                    conditionexpr.delete(0, index + 1);
                    result = new AndCondition(this);
                    ((ConditionWithMultipleOperands)result).addOperands(op1, getConditionByExpr(conditionexpr));
                    break;
                case '|':
                    // or-condition
                    op1 = conditionsmap.get(conditionexpr.substring(0, index));
                    conditionexpr.delete(0, index + 1);
                    result = new OrCondition(this);
                    ((ConditionWithMultipleOperands)result).addOperands(op1, getConditionByExpr(conditionexpr));

                    break;
                case '\\':
                    // xor-condition
                    op1 = conditionsmap.get(conditionexpr.substring(0, index));
                    conditionexpr.delete(0, index + 1);
                    result = new XorCondition(this);
                    ((ConditionWithMultipleOperands)result).addOperands(op1, getConditionByExpr(conditionexpr));
                    break;
                case '!':
                    // not-condition
                    if (index > 0)
                    {
                        logger.warning("! operator only allowed at position 0");
                    }
                    else
                    {
                        // delete not symbol
                        conditionexpr.deleteCharAt(index);
                        result = NotCondition.createFromCondition(
                                getConditionByExpr(conditionexpr),
                                this);
                    }
                    break;
                default:
                    // do nothing
            }
            index++;
        }
        if (conditionexpr.length() > 0)
        {
            result = conditionsmap.get(conditionexpr.toString());
            if (result != null)
            {
                result.setInstalldata(installdata);
                conditionexpr.delete(0, conditionexpr.length());
            }
        }
        return result;
    }

    @Override
    public boolean isConditionTrue(String id, AutomatedInstallData installData)
    {
        Condition cond = getCondition(id);
        if (cond != null)
        {
            return isConditionTrue(cond, installData);
        }
        logger.warning("Condition " + id + " not found");
        return false;
    }

    @Override
    public boolean isConditionTrue(Condition cond, AutomatedInstallData installData)
    {
        if (cond != null)
        {
            if (installData != null)
            {
                cond.setInstalldata(installData);
            }
            return isConditionTrue(cond);
        }
        return false;
    }

    @Override
    public boolean isConditionTrue(String id)
    {
        Condition cond = getCondition(id);
        if (cond != null)
        {
            return isConditionTrue(cond);
        }
        logger.warning("Condition " + id + " not found");
        return false;
    }

    @Override
    public boolean isConditionTrue(Condition cond)
    {
        if (cond.getInstallData() == null)
        {
            cond.setInstalldata(this.installdata);
        }
        boolean value = cond.isTrue();
        logger.fine("Condition " + cond.getId() + ": " + Boolean.toString(value));
        return value;
    }

    /**
     * Can a panel be shown?
     *
     * @param panelid   - id of the panel, which should be shown
     * @param variables - the variables
     * @return true - there is no condition or condition is met false - there is a condition and the
     *         condition was not met
     */
    @Override
    public boolean canShowPanel(String panelid, Properties variables)
    {
        if (!this.panelconditions.containsKey(panelid))
        {
            logger.fine("Panel " + panelid + " unconditionally activated");
            return true;
        }
        Condition condition = getCondition(this.panelconditions.get(panelid));
        boolean b = condition.isTrue();
        logger.fine("Panel " + panelid + ": activation depends on condition "
        + condition.getId() + " -> " + b);
        return b;
    }

    /**
     * Is the installation of a pack possible?
     *
     * @param packid
     * @param variables
     * @return true - there is no condition or condition is met false - there is a condition and the
     *         condition was not met
     */
    @Override
    public boolean canInstallPack(String packid, Properties variables)
    {
        if (packid == null)
        {
            return true;
        }
        if (!this.packconditions.containsKey(packid))
        {
            logger.fine("Package " + packid + " unconditionally installable");
            return true;
        }
        Condition condition = getCondition(this.packconditions.get(packid));
        boolean b = condition.isTrue();
        logger.fine("Package " + packid + ": installation depends on condition "
        + condition.getId() + " -> " + b);
        return b;
    }

    /**
     * Is an optional installation of a pack possible if the condition is not met?
     *
     * @param packid
     * @param variables
     * @return
     */
    @Override
    public boolean canInstallPackOptional(String packid, Properties variables)
    {
        if (!this.optionalpackconditions.containsKey(packid))
        {
            logger.fine("Package " + packid + " unconditionally installable");
            return false;
        }
        else
        {
            logger.fine("Package " + packid + " optional installation possible");
            return true;
        }
    }

    /**
     * @param condition
     */
    @Override
    public void addCondition(Condition condition)
    {
        if (condition != null)
        {
            String id = condition.getId();
            if (conditionsmap.containsKey(id))
            {
                logger.warning("Condition " + id + " already registered");
            }
            else
            {
                conditionsmap.put(id, condition);
            }
        }
        else
        {
            logger.warning("Could not add condition, was null");
        }
    }

    @Override
    public void writeRulesXML(OutputStream out)
    {
        XMLWriter xmlOut = new XMLWriter();
        xmlOut.setOutput(out);
        XMLElementImpl conditionsel = new XMLElementImpl("conditions");
        for (Condition condition : conditionsmap.values())
        {
            IXMLElement conditionEl = createConditionElement(condition, conditionsel);
            condition.makeXMLData(conditionEl);
            conditionsel.addChild(conditionEl);
        }
        logger.fine("Writing generated conditions specification");
        try
        {
            xmlOut.write(conditionsel);
        }
        catch (XMLException e)
        {
            throw new IzPackException(e);
        }
    }

    @Override
    public IXMLElement createConditionElement(Condition condition, IXMLElement root)
    {
        XMLElementImpl xml = new XMLElementImpl("condition", root);
        xml.setAttribute("id", condition.getId());
        xml.setAttribute("type", condition.getClass().getCanonicalName());
        return xml;
    }
}
