package com.zzg.mybatis.generator.plugins;

import org.mybatis.generator.api.*;
import org.mybatis.generator.api.dom.OutputUtilities;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.codegen.mybatis3.ListUtilities;
import org.mybatis.generator.codegen.mybatis3.MyBatis3FormattingUtilities;
import org.mybatis.generator.exception.ShellException;
import org.mybatis.generator.internal.DefaultShellCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.mybatis.generator.internal.util.StringUtility.stringHasValue;

/**
 * Project: mybatis-generator-gui
 *
 * @author slankka on 2018/3/11.
 */
public class CommonDAOInterfacePlugin extends PluginAdapter {

    private static final String DEFAULT_DAO_SUPER_CLASS = ".MyBatisBaseDao";
    private static final FullyQualifiedJavaType SERIALIZEBLE_TYPE = new FullyQualifiedJavaType("java.io.Serializable");

    private List<Method> methods = new ArrayList<>();

    private ShellCallback shellCallback = null;

    public CommonDAOInterfacePlugin() {
        shellCallback = new DefaultShellCallback(false);
    }
    
    private boolean isUseExample() {
    	return "true".equals(getProperties().getProperty("useExample"));
	}

    @Override
    public List<GeneratedJavaFile> contextGenerateAdditionalJavaFiles(IntrospectedTable introspectedTable) {
        boolean hasPk = introspectedTable.hasPrimaryKeyColumns();
        JavaFormatter javaFormatter = context.getJavaFormatter();
        String daoTargetDir = context.getJavaClientGeneratorConfiguration().getTargetProject();
        String daoTargetPackage = context.getJavaClientGeneratorConfiguration().getTargetPackage();
        List<GeneratedJavaFile> mapperJavaFiles = new ArrayList<>();
        String javaFileEncoding = context.getProperty("javaFileEncoding");
        Interface mapperInterface = new Interface(daoTargetPackage + DEFAULT_DAO_SUPER_CLASS);

        if (stringHasValue(daoTargetPackage)) {
            mapperInterface.addImportedType(SERIALIZEBLE_TYPE);

            mapperInterface.setVisibility(JavaVisibility.PUBLIC);
            mapperInterface.addJavaDocLine("/**");
            mapperInterface.addJavaDocLine(" * " + "DAO公共基类，由MybatisGenerator自动生成请勿修改");
            mapperInterface.addJavaDocLine(" * " + "@param <Model> The Model Class 这里是泛型不是Model类");
            mapperInterface.addJavaDocLine(" * " + "@param <PK> The Primary Key Class 如果是无主键，则可以用Model来跳过，如果是多主键则是Key类");
			if (isUseExample()) {
				mapperInterface.addJavaDocLine(" * " + "@param <E> The Example Class");
			}
            mapperInterface.addJavaDocLine(" */");

            FullyQualifiedJavaType daoBaseInterfaceJavaType = mapperInterface.getType();
            daoBaseInterfaceJavaType.addTypeArgument(new FullyQualifiedJavaType("Model"));
            daoBaseInterfaceJavaType.addTypeArgument(new FullyQualifiedJavaType("PK extends Serializable"));
			if (isUseExample()) {
				daoBaseInterfaceJavaType.addTypeArgument(new FullyQualifiedJavaType("E"));
			}

            if (!this.methods.isEmpty()) {
                for (Method method : methods) {
                    mapperInterface.addMethod(method);
                }
            }

            //Method batchUpdateMethod = createNoReturnMethod("batchUpdate");
            //Method batchUpdateSelectiveMethod = createNoReturnMethod("batchUpdateSelective");
            Method batchInsertMethod = createNoReturnMethod("batchInsert");

            //mapperInterface.addMethod(batchUpdateMethod);
            //mapperInterface.addMethod(batchUpdateSelectiveMethod);
            mapperInterface.addMethod(batchInsertMethod);

            mapperInterface.addImportedType(new FullyQualifiedJavaType("java.util.List"));

            List<GeneratedJavaFile> generatedJavaFiles = introspectedTable.getGeneratedJavaFiles();
            for (GeneratedJavaFile generatedJavaFile : generatedJavaFiles) {
                CompilationUnit compilationUnit = generatedJavaFile.getCompilationUnit();
                FullyQualifiedJavaType type = compilationUnit.getType();
                String modelName = type.getShortName();
                if (modelName.endsWith("DAO")) {
                }
            }
            GeneratedJavaFile mapperJavafile = new GeneratedJavaFile(mapperInterface, daoTargetDir, javaFileEncoding, javaFormatter);
            try {
                File mapperDir = shellCallback.getDirectory(daoTargetDir, daoTargetPackage);
                File mapperFile = new File(mapperDir, mapperJavafile.getFileName());
                // 文件不存在
                if (!mapperFile.exists()) {
                    mapperJavaFiles.add(mapperJavafile);
                }
            } catch (ShellException e) {
                e.printStackTrace();
            }
        }
        return mapperJavaFiles;
    }

    @Override
    public boolean clientGenerated(Interface interfaze, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        interfaze.addJavaDocLine("/**");
        interfaze.addJavaDocLine(" * " + interfaze.getType().getShortName() + " 继承基类");
        interfaze.addJavaDocLine(" */");

        String daoSuperClass = interfaze.getType().getPackageName() + DEFAULT_DAO_SUPER_CLASS;
        FullyQualifiedJavaType daoSuperType = new FullyQualifiedJavaType(daoSuperClass);

        String targetPackage = introspectedTable.getContext().getJavaModelGeneratorConfiguration().getTargetPackage();

        String domainObjectName = introspectedTable.getTableConfiguration().getDomainObjectName();
        FullyQualifiedJavaType baseModelJavaType = new FullyQualifiedJavaType(targetPackage + "." + domainObjectName);
        daoSuperType.addTypeArgument(baseModelJavaType);

        FullyQualifiedJavaType primaryKeyTypeJavaType = null;
        if (introspectedTable.getPrimaryKeyColumns().size() > 1) {
            primaryKeyTypeJavaType = new FullyQualifiedJavaType(targetPackage + "." + domainObjectName + "Key");
        } else if(introspectedTable.hasPrimaryKeyColumns()) {
            primaryKeyTypeJavaType = introspectedTable.getPrimaryKeyColumns().get(0).getFullyQualifiedJavaType();
        } else {
            primaryKeyTypeJavaType = baseModelJavaType;
        }
        daoSuperType.addTypeArgument(primaryKeyTypeJavaType);
		interfaze.addImportedType(primaryKeyTypeJavaType);

		if (isUseExample()) {
			String exampleType = introspectedTable.getExampleType();
			FullyQualifiedJavaType exampleTypeJavaType = new FullyQualifiedJavaType(exampleType);
			daoSuperType.addTypeArgument(exampleTypeJavaType);
			interfaze.addImportedType(exampleTypeJavaType);
		}
        interfaze.addImportedType(baseModelJavaType);
        interfaze.addImportedType(daoSuperType);
        interfaze.addSuperInterface(daoSuperType);
        return true;
    }

    @Override
    public boolean validate(List<String> list) {
        return true;
    }

    @Override
    public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
        XmlElement parentElement = document.getRootElement();

        //addBatchUpdateElements(parentElement, introspectedTable);
        //addBatchUpdateSelectiveElements(parentElement, introspectedTable);
        addBatchInsertElements(parentElement, introspectedTable);

        return super.sqlMapDocumentGenerated(document, introspectedTable);
    }

    private void interceptExampleParam(Method method) {
		if (isUseExample()) {
			method.getParameters().clear();
			method.addParameter(new Parameter(new FullyQualifiedJavaType("E"), "example"));
			methods.add(method);
		}
    }

    private void interceptPrimaryKeyParam(Method method) {
        method.getParameters().clear();
        method.addParameter(new Parameter(new FullyQualifiedJavaType("PK"), "id"));
        methods.add(method);
    }

    private void interceptModelParam(Method method) {
        method.getParameters().clear();
        method.addParameter(new Parameter(new FullyQualifiedJavaType("Model"), "record"));
        methods.add(method);
    }

    private void interceptModelAndExampleParam(Method method) {
		if (isUseExample()) {
			List<Parameter> parameters = method.getParameters();
			if (parameters.size() == 1) {
				interceptExampleParam(method);
			}else{
				method.getParameters().clear();
				Parameter parameter1 = new Parameter(new FullyQualifiedJavaType("Model"), "record");
				parameter1.addAnnotation("@Param(\"record\")");
				method.addParameter(parameter1);

				Parameter parameter2 = new Parameter(new FullyQualifiedJavaType("E"), "example");
				parameter2.addAnnotation("@Param(\"example\")");
				method.addParameter(parameter2);
				methods.add(method);
			}
		}
    }

    @Override
    public boolean clientCountByExampleMethodGenerated(Method method,
                                                       Interface interfaze, IntrospectedTable introspectedTable) {
//        interface
		if (isUseExample()) {
			interceptExampleParam(method);
		}
		return false;
	}


    @Override
    public boolean clientDeleteByExampleMethodGenerated(Method method,
                                                        Interface interfaze, IntrospectedTable introspectedTable) {
        if (isUseExample()) {
			interceptExampleParam(method);
		}
        return false;
    }

    @Override
    public boolean clientDeleteByPrimaryKeyMethodGenerated(Method method,
                                                           Interface interfaze, IntrospectedTable introspectedTable) {
    	interceptPrimaryKeyParam(method);
        return false;
    }

    @Override
    public boolean clientInsertMethodGenerated(Method method, Interface interfaze,
                                                  IntrospectedTable introspectedTable) {
        interceptModelParam(method);
        return false;
    }

    @Override
    public boolean clientSelectByExampleWithBLOBsMethodGenerated(Method method,
                                                                 Interface interfaze, IntrospectedTable introspectedTable) {
        if (isUseExample()) {
			interceptExampleParam(method);
			method.setReturnType(new FullyQualifiedJavaType("List<Model>"));
		}
        return false;
    }

    @Override
    public boolean clientSelectByExampleWithoutBLOBsMethodGenerated(Method method,
                                                                    Interface interfaze, IntrospectedTable introspectedTable) {
        if (isUseExample()) {
			interceptExampleParam(method);
			method.setReturnType(new FullyQualifiedJavaType("List<Model>"));
		}
        return false;
    }

    @Override
    public boolean clientSelectByPrimaryKeyMethodGenerated(Method method,
                                                           Interface interfaze, IntrospectedTable introspectedTable) {
    	interceptPrimaryKeyParam(method);
        method.setReturnType(new FullyQualifiedJavaType("Model"));
        return false;
    }

    @Override
    public boolean clientUpdateByExampleSelectiveMethodGenerated(Method method,
                                                                 Interface interfaze, IntrospectedTable introspectedTable) {
        if (isUseExample()) {
			interceptModelAndExampleParam(method);
		}
        return false;
    }

    @Override
    public boolean clientUpdateByExampleWithBLOBsMethodGenerated(Method method,
                                                                 Interface interfaze, IntrospectedTable introspectedTable) {
        if (isUseExample()) {
			interceptModelAndExampleParam(method);
		}
        return false;
    }

    @Override
    public boolean clientUpdateByExampleWithoutBLOBsMethodGenerated(Method method,
                                                                    Interface interfaze, IntrospectedTable introspectedTable) {
        if (isUseExample()) {
			interceptModelAndExampleParam(method);
		}
        return false;
    }

    @Override
    public boolean clientUpdateByPrimaryKeySelectiveMethodGenerated(Method method,
                                                                    Interface interfaze, IntrospectedTable introspectedTable) {
        interceptModelParam(method);
        return false;
    }

    /**
    @Override
    public boolean clientUpdateByExampleWithoutBLOBsMethodGenerated(Method method, IntrospectedTable introspectedTable) {
        if (isUseExample()) {
			interceptModelAndExampleParam(method);
		}
        return false;
    }

    @Override
    public boolean clientUpdateByExampleSelectiveMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        if (isUseExample()) {
			interceptModelAndExampleParam(method);
		}
        return false;
    }
    */

    @Override
    public boolean clientUpdateByPrimaryKeyWithBLOBsMethodGenerated(Method method,
                                                                    Interface interfaze, IntrospectedTable introspectedTable) {
    	interceptModelParam(method);
        return false;
    }

    @Override
    public boolean clientUpdateByPrimaryKeyWithoutBLOBsMethodGenerated(
            Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        interceptModelParam(method);
        return false;
    }

    @Override
    public boolean clientInsertSelectiveMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        interceptModelParam(method);
        return false;
    }



    private void addBatchUpdateElements(XmlElement parentElement, IntrospectedTable introspectedTable) {
        addBatchUpdateElements(parentElement, introspectedTable, false);
    }

    private void addBatchUpdateSelectiveElements(XmlElement parentElement, IntrospectedTable introspectedTable) {
        addBatchUpdateElements(parentElement, introspectedTable, true);
    }

    private void addBatchUpdateElements(XmlElement parentElement, IntrospectedTable introspectedTable, boolean selective) {
        XmlElement answer = new XmlElement("update");

        if (selective) {
            answer.addAttribute(new Attribute("id", "batchUpdateSelective"));
        } else {
            answer.addAttribute(new Attribute("id", "batchUpdate"));
        }

        answer.addAttribute(new Attribute("parameterType", "java.util.List"));

        StringBuilder sb = new StringBuilder();

        sb.append("update into ");
        sb.append(introspectedTable.getFullyQualifiedTableNameAtRuntime());
        answer.addElement(new TextElement(sb.toString()));

        XmlElement firstTrimElement = new XmlElement("trim");
        firstTrimElement.addAttribute(new Attribute("prefix", "set"));
        firstTrimElement.addAttribute(new Attribute("suffixOverrides", ","));

        IntrospectedColumn primaryKeyColumn = introspectedTable.getPrimaryKeyColumns().get(0);

        for (IntrospectedColumn introspectedColumn : ListUtilities.removeGeneratedAlwaysColumns(introspectedTable
                .getNonPrimaryKeyColumns())) {

            if (introspectedColumn.isSequenceColumn()
                    || introspectedColumn.getFullyQualifiedJavaType().isPrimitive()) {
                // if it is a sequence column, it is not optional
                // This is required for MyBatis3 because MyBatis3 parses
                // and calculates the SQL before executing the selectKey

                // if it is primitive, we cannot do a null check

                continue;
            }

            sb.setLength(0);
            sb.append(introspectedColumn.getJavaProperty());
            sb.append(" = case"); //$NON-NLS-1$
            XmlElement secondTrimElement = new XmlElement("trim");
            secondTrimElement.addAttribute(new Attribute("prefix", sb.toString()));
            secondTrimElement.addAttribute(new Attribute("suffix", "end,"));

            XmlElement foreachElement = new XmlElement("foreach");
            secondTrimElement.addAttribute(new Attribute("collection", "list"));
            secondTrimElement.addAttribute(new Attribute("index", "id"));
            secondTrimElement.addAttribute(new Attribute("item", "item"));

            sb.setLength(0);
            sb.append("when id = ");
            sb.append(getItemParameterClause(primaryKeyColumn));
            sb.append(" then ");
            sb.append(getItemParameterClause(introspectedColumn));
            TextElement textElement = new TextElement(sb.toString());

            if (selective) {
                XmlElement ifElement = new XmlElement("if");
                sb.setLength(0);
                sb.append("item.");
                sb.append(introspectedColumn.getJavaProperty());
                sb.append(" != null");
                ifElement.addAttribute(new Attribute("test", sb.toString()));
                ifElement.addElement(textElement);
                foreachElement.addElement(ifElement);
            } else {
                foreachElement.addElement(textElement);
            }

            secondTrimElement.addElement(foreachElement);

            firstTrimElement.addElement(secondTrimElement);
        }
        answer.addElement(firstTrimElement);

        sb.setLength(0);
        sb.append("where ");
        sb.append(primaryKeyColumn.getJavaProperty());
        sb.append(" in");
        answer.addElement(new TextElement(sb.toString()));

        XmlElement valuesForeachElement = new XmlElement("foreach");
        valuesForeachElement.addAttribute(new Attribute("open", "("));
        valuesForeachElement.addAttribute(new Attribute("close", ")"));
        valuesForeachElement.addAttribute(new Attribute("collection", "list"));
        valuesForeachElement.addAttribute(new Attribute("item", "item"));
        valuesForeachElement.addAttribute(new Attribute("separator", ", "));
        sb.setLength(0);
        sb.append(getItemParameterClause(primaryKeyColumn));
        valuesForeachElement.addElement(new TextElement(sb.toString()));

        answer.addElement(valuesForeachElement);

        XmlElement valuesTrimElement = new XmlElement("trim"); //$NON-NLS-1$
        valuesTrimElement.addAttribute(new Attribute("prefix", "values (")); //$NON-NLS-1$ //$NON-NLS-2$
        valuesTrimElement.addAttribute(new Attribute("suffix", ")")); //$NON-NLS-1$ //$NON-NLS-2$
        valuesTrimElement.addAttribute(new Attribute("suffixOverrides", ",")); //$NON-NLS-1$ //$NON-NLS-2$
        answer.addElement(valuesTrimElement);

        parentElement.addElement(answer);
    }

    private void addBatchInsertElements(XmlElement parentElement, IntrospectedTable introspectedTable) {
        XmlElement answer = new XmlElement("insert");

        answer.addAttribute(new Attribute("id", "batchInsert"));

        answer.addAttribute(new Attribute("parameterType", "java.util.List"));

        StringBuilder insertClause = new StringBuilder();

        insertClause.append("insert into ");
        insertClause.append(introspectedTable.getFullyQualifiedTableNameAtRuntime());
        insertClause.append(" (");

        StringBuilder valuesClause = new StringBuilder();
        valuesClause.append("(");

        List<String> valuesClauses = new LinkedList<>();
        List<IntrospectedColumn> columns = ListUtilities.removeIdentityAndGeneratedAlwaysColumns(introspectedTable.getAllColumns());
        for (int i = 0; i < columns.size(); i++) {
            IntrospectedColumn introspectedColumn = columns.get(i);

            insertClause.append(MyBatis3FormattingUtilities.getEscapedColumnName(introspectedColumn));
            valuesClause.append(getItemParameterClause(introspectedColumn));
            if (i + 1 < columns.size()) {
                insertClause.append(", ");
                valuesClause.append(", ");
            }

            if (valuesClause.length() > 80) {
                answer.addElement(new TextElement(insertClause.toString()));
                insertClause.setLength(0);
                OutputUtilities.xmlIndent(insertClause, 1);

                valuesClauses.add(valuesClause.toString());
                valuesClause.setLength(0);
                OutputUtilities.xmlIndent(valuesClause, 1);
            }
        }

        insertClause.append(')');
        answer.addElement(new TextElement(insertClause.toString()));

        answer.addElement(new TextElement("values"));

        valuesClause.append(')');
        valuesClauses.add(valuesClause.toString());

        XmlElement foreach = new XmlElement("foreach");
        foreach.addAttribute(new Attribute("collection", "list"));
        foreach.addAttribute(new Attribute("item", "item"));
        foreach.addAttribute(new Attribute("separator", ","));

        for (String clause : valuesClauses) {
            foreach.addElement(new TextElement(clause));
        }

        answer.addElement(foreach);

        parentElement.addElement(answer);
    }

    public static String getItemParameterClause(IntrospectedColumn introspectedColumn) {
        return getItemParameterClause(introspectedColumn, null);
    }

    public static String getItemParameterClause(IntrospectedColumn introspectedColumn, String prefix) {
        StringBuilder sb = new StringBuilder();

        sb.append("#{item."); //$NON-NLS-1$
        sb.append(introspectedColumn.getJavaProperty(prefix));
        sb.append(",jdbcType="); //$NON-NLS-1$
        sb.append(introspectedColumn.getJdbcTypeName());

        if (stringHasValue(introspectedColumn.getTypeHandler())) {
            sb.append(",typeHandler="); //$NON-NLS-1$
            sb.append(introspectedColumn.getTypeHandler());
        }

        sb.append('}');

        return sb.toString();
    }

    private Method createNoReturnMethod(String name) {
        Method method = new Method(name);
        method.setVisibility(JavaVisibility.PUBLIC);
        addParameter(method);
        return method;
    }

    private void addParameter(Method method) {
        method.getParameters().clear();
        method.addParameter(new Parameter(new FullyQualifiedJavaType("List<Model>"), "list"));
    }
}
