/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
 *    This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License. You may obtain a copy
 *    of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under
 *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *    OF ANY KIND, either express or implied. See the License for the specific language
 *    governing permissions and limitations under the License.
 *
 ******************************************************************************/

package com.adobe.cq.commerce.core.components.models.graphql;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Stack;
import java.util.StringJoiner;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Element;

import com.noctarius.graphquul.Parser;
import com.noctarius.graphquul.ast.Argument;
import com.noctarius.graphquul.ast.BooleanLiteral;
import com.noctarius.graphquul.ast.DefaultValue;
import com.noctarius.graphquul.ast.Directive;
import com.noctarius.graphquul.ast.DirectiveDefinition;
import com.noctarius.graphquul.ast.DirectiveLocation;
import com.noctarius.graphquul.ast.Document;
import com.noctarius.graphquul.ast.EnumOrNameLiteral;
import com.noctarius.graphquul.ast.EnumTypeDefinition;
import com.noctarius.graphquul.ast.EnumValueDefinition;
import com.noctarius.graphquul.ast.Field;
import com.noctarius.graphquul.ast.FieldDefinition;
import com.noctarius.graphquul.ast.FloatLiteral;
import com.noctarius.graphquul.ast.FragmentDefinition;
import com.noctarius.graphquul.ast.FragmentSpread;
import com.noctarius.graphquul.ast.ImplementsInterface;
import com.noctarius.graphquul.ast.InlineFragment;
import com.noctarius.graphquul.ast.InputObjectTypeDefinition;
import com.noctarius.graphquul.ast.InputValueDefinition;
import com.noctarius.graphquul.ast.IntegerLiteral;
import com.noctarius.graphquul.ast.InterfaceTypeDefinition;
import com.noctarius.graphquul.ast.ListType;
import com.noctarius.graphquul.ast.ListValue;
import com.noctarius.graphquul.ast.Node;
import com.noctarius.graphquul.ast.ObjectField;
import com.noctarius.graphquul.ast.ObjectTypeDefinition;
import com.noctarius.graphquul.ast.ObjectValue;
import com.noctarius.graphquul.ast.OperationDefinition;
import com.noctarius.graphquul.ast.OperationTypeDefinition;
import com.noctarius.graphquul.ast.ScalarTypeDefinition;
import com.noctarius.graphquul.ast.SchemaDefinition;
import com.noctarius.graphquul.ast.StringLiteral;
import com.noctarius.graphquul.ast.Type;
import com.noctarius.graphquul.ast.TypeExtensionDefinition;
import com.noctarius.graphquul.ast.UnionMember;
import com.noctarius.graphquul.ast.UnionTypeDefinition;
import com.noctarius.graphquul.ast.Variable;
import com.noctarius.graphquul.ast.VariableDefinition;
import com.noctarius.graphquul.visitor.ASTVisitor;

public class GraphqlContentBuilder {

    private final org.w3c.dom.Document document;

    public GraphqlContentBuilder() throws Exception {
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
        document = documentBuilder.newDocument();
    }

    public static void main(String[] args) throws Exception {

        System.out.println("Processing " + args[0]);

        Path file = Paths.get(args[0]);
        Stream<String> lines = Files.lines(file, Charset.forName("UTF-8"));
        String query = lines.collect(() -> new StringJoiner("\n"), StringJoiner::add, StringJoiner::merge).toString();
        Document document = Parser.parse(query);
        DOMSource domSource = buildContent(document);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        Path dir = file.getParent().resolve("graphql");
        dir.toFile().mkdir();

        Path content = dir.resolve(".content.xml");
        StreamResult streamResult = new StreamResult(content.toFile());
        transformer.transform(domSource, streamResult);
        System.out.println("Saved " + content);

    }

    public static DOMSource buildContent(Node node) throws Exception {
        GraphqlContentBuilder graphqlContentBuilder = new GraphqlContentBuilder();
        ASTVisitor visitor = graphqlContentBuilder.new TreeVisitor();
        visitor.visit(node);
        return new DOMSource(graphqlContentBuilder.document);
    }

    private class TreeVisitor implements ASTVisitor {
        private Stack<Element> stack = new Stack<>();

        @Override
        public void visit(Node node) {
            node.acceptVisitor(this);
        }

        @Override
        public void visit(Document document) {

            Element root = createElement("jcr:root");
            root.setAttribute("xmlns:jcr", "http://www.jcp.org/jcr/1.0");
            GraphqlContentBuilder.this.document.appendChild(root);

            if (document.hasDefinitions()) {
                stack.push(root);
                document.definitions().forEach(d -> d.acceptVisitor(this));
                stack.pop();
            }
        }

        @Override
        public void visit(OperationDefinition operationDefinition) {
            Element node = createElement(operationDefinition.operationType().name().toLowerCase());
            stack.peek().appendChild(node);

            if (operationDefinition.hasVariableDefinitions()) {
                Element vars = createElement("_variables_");
                node.appendChild(vars);

                stack.push(vars);
                operationDefinition.variableDefinitions().forEach(n -> n.acceptVisitor(this));
                stack.pop();
            }

            if (operationDefinition.hasSelections()) {
                stack.push(node);
                operationDefinition.selections().forEach(n -> n.acceptVisitor(this));
                stack.pop();
            }
        }

        @Override
        public void visit(VariableDefinition variableDefinition) {
            Element el = createElement(variableDefinition.variable().name());
            el.setAttribute("type", "variabledefinition");
            final Type type = variableDefinition.type();
            String varType = typeToString(type);
            el.setAttribute("variableType", varType);
            DefaultValue defaultValue = variableDefinition.defaultValue();
            if (defaultValue != null) {
                el.setAttribute("default", defaultValue.value().toString());
            }
            stack.peek().appendChild(el);
        }

        @Override
        public void visit(FragmentDefinition fragmentDefinition) {
            Element el = createElement(fragmentDefinition.name());
            el.setAttribute("type", "fragmentdefinition");
            el.setAttribute("on", fragmentDefinition.typeCondition());
            stack.peek().appendChild(el);

            if (fragmentDefinition.hasSelections()) {
                stack.push(el);
                fragmentDefinition.selections().forEach(n -> n.acceptVisitor(this));
                stack.pop();
            }
        }

        @Override
        public void visit(Field field) {
            Element el = createElement(field.name());
            if (field.alias() != null) {
                el.setAttribute("alias", field.alias());
            }

            stack.peek().appendChild(el);

            if (field.hasArguments()) {
                Element args = createElement("_arguments_");
                el.appendChild(args);

                stack.push(args);
                field.arguments().forEach(n -> n.acceptVisitor(this));
                stack.pop();
            }

            if (field.hasSelections()) {
                stack.push(el);
                field.selections().forEach(n -> n.acceptVisitor(this));
                stack.pop();
            }
        }

        @Override
        public void visit(Argument argument) {
            Element el = createElement(argument.name());
            el.setAttribute("type", "argument");
            stack.peek().appendChild(el);

            stack.push(el);
            argument.value().acceptVisitor(this);
            stack.pop();
        }

        @Override
        public void visit(InlineFragment inlineFragment) {
            Element el = createElement(inlineFragment.typeCondition());
            el.setAttribute("type", "inlinefragment");

            stack.peek().appendChild(el);

            if (inlineFragment.hasSelections()) {
                stack.push(el);
                inlineFragment.selections().forEach(n -> n.acceptVisitor(this));
                stack.pop();
            }
        }

        @Override
        public void visit(FragmentSpread fragmentSpread) {
            Element el = createElement(fragmentSpread.name());
            el.setAttribute("type", "fragment");
            stack.peek().appendChild(el);
        }

        @Override
        public void visit(ObjectField objectField) {
            Element el = createElement(objectField.name());
            el.setAttribute("type", "objectfield");
            stack.peek().appendChild(el);

            stack.push(el);
            objectField.value().acceptVisitor(this);
            stack.pop();
        }

        @Override
        public void visit(ObjectValue objectValue) {
            if (objectValue.hasObjectFields()) {
                objectValue.objectFields().forEach(n -> n.acceptVisitor(this));
            }
        }

        @Override
        public void visit(StringLiteral stringLiteral) {
            stack.peek().setAttribute("value", stringLiteral.value());
        }

        @Override
        public void visit(InputObjectTypeDefinition inputObjectTypeDefinition) {
            inputObjectTypeDefinition.acceptVisitor(this);
        }

        @Override
        public void visit(InputValueDefinition inputValueDefinition) {
            inputValueDefinition.acceptVisitor(this);
        }

        @Override
        public void visit(FieldDefinition fieldDefinition) {
            fieldDefinition.type().acceptVisitor(this);
        }

        @Override
        public void visit(DirectiveDefinition directiveDefinition) {
            directiveDefinition.directiveLocations().forEach(d -> d.acceptVisitor(this));
        }

        @Override
        public void visit(Variable variable) {
            stack.peek().setAttribute("value", "$" + variable.name());
        }

        // still unsupported
        @Override
        public void visit(EnumTypeDefinition enumTypeDefinition) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(EnumValueDefinition enumValueDefinition) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(BooleanLiteral booleanLiteral) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(DefaultValue defaultValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(Directive directive) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(DirectiveLocation directiveLocation) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(EnumOrNameLiteral enumOrNameLiteral) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(FloatLiteral floatLiteral) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(ImplementsInterface implementsInterface) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(IntegerLiteral integerLiteral) {
            stack.peek().setAttribute("value", "{Long}" + integerLiteral.value());
        }

        @Override
        public void visit(InterfaceTypeDefinition interfaceTypeDefinition) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(ListType listType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(ListValue listValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(ObjectTypeDefinition objectTypeDefinition) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(OperationTypeDefinition operationTypeDefinition) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(ScalarTypeDefinition scalarTypeDefinition) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(SchemaDefinition schemaDefinition) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(Type type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(TypeExtensionDefinition typeExtensionDefinition) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(UnionMember unionMember) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(UnionTypeDefinition unionTypeDefinition) {
            throw new UnsupportedOperationException();
        }

        // private methods
        private String typeToString(Type type) {
            String name;
            if (type instanceof ListType) {
                ListType listType = (ListType) type;
                name = '[' + typeToString(listType.componentType()) + ']';
            } else {
                name = type.name();
            }

            if (!type.nullable()) {
                name += '!';
            }

            return name;
        }
    }

    private Element createElement(String s) {
        final Element element = GraphqlContentBuilder.this.document.createElement(s);
        element.setAttribute("jcr:primaryType", "nt:unstructured");
        return element;
    }
}
