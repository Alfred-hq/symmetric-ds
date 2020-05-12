/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.symmetric.db.oracle;

import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.db.model.Table;
import org.jumpmind.symmetric.common.ParameterConstants;
import org.jumpmind.symmetric.db.AbstractTriggerTemplate;
import org.jumpmind.symmetric.db.ISymmetricDialect;

public class OracleTriggerTemplate extends AbstractTriggerTemplate {

    public OracleTriggerTemplate(ISymmetricDialect symmetricDialect) {
        super(symmetricDialect);
        // @formatter:off

        emptyColumnTemplate = "''" ;
        stringColumnTemplate = "decode($(tableAlias).\"$(columnName)\", null, $(oracleToClob)'', '\"'||replace(replace($(oracleToClob)$(tableAlias).\"$(columnName)\",'\\','\\\\'),'\"','\\\"')||'\"')" ;
        geometryColumnTemplate = "case when $(tableAlias).\"$(columnName)\" is null then to_clob('') else '\"'||replace(replace(SDO_UTIL.TO_WKTGEOMETRY($(tableAlias).\"$(columnName)\"),'\\','\\\\'),'\"','\\\"')||'\"' end";
        numberColumnTemplate = "decode($(tableAlias).\"$(columnName)\", null, '', '\"'||" + getNumberConversionString() + "||'\"')" ;
        datetimeColumnTemplate = "decode($(tableAlias).\"$(columnName)\", null, '', concat(concat('\"',to_char($(tableAlias).\"$(columnName)\", 'YYYY-MM-DD HH24:MI:SS.FF9')),'\"'))" ;
        dateTimeWithTimeZoneColumnTemplate = "decode($(tableAlias).\"$(columnName)\", null, '', concat(concat('\"',to_char($(tableAlias).\"$(columnName)\", 'YYYY-MM-DD HH24:MI:SS.FF9 TZH:TZM')),'\"'))" ;
        dateTimeWithLocalTimeZoneColumnTemplate = "decode($(tableAlias).\"$(columnName)\", null, '', concat(concat('\"',to_char(cast($(tableAlias).\"$(columnName)\" as timestamp), 'YYYY-MM-DD HH24:MI:SS.FF9')),'\"'))" ;
        timeColumnTemplate = "decode($(tableAlias).\"$(columnName)\", null, '', concat(concat('\"',to_char($(tableAlias).\"$(columnName)\", 'YYYY-MM-DD HH24:MI:SS','NLS_CALENDAR=''GREGORIAN''')),'\"'))" ;
        dateColumnTemplate = "decode($(tableAlias).\"$(columnName)\", null, '', concat(concat('\"',to_char($(tableAlias).\"$(columnName)\", 'YYYY-MM-DD HH24:MI:SS','NLS_CALENDAR=''GREGORIAN''')),'\"'))" ;
        clobColumnTemplate = "case when $(tableAlias).\"$(columnName)\" is null then null else '\"'||replace(replace($(tableAlias).\"$(columnName)\",'\\','\\\\'),'\"','\\\"')||'\"' end" ;
        blobColumnTemplate = "decode(dbms_lob.getlength($(tableAlias).\"$(columnName)\"), null, to_clob(''), '\"'||$(prefixName)_blob2clob($(tableAlias).\"$(columnName)\")||'\"')" ;
        longColumnTemplate = "$(oracleToClob)'\"\\b\"'";
        booleanColumnTemplate = "decode($(tableAlias).\"$(columnName)\", null, '', '\"'||cast($(tableAlias).\"$(columnName)\" as number("+symmetricDialect.getTemplateNumberPrecisionSpec()+"))||'\"')" ;
        xmlColumnTemplate = "decode(dbms_lob.getlength(extract($(tableAlias).\"$(columnName)\", '/').getclobval()), null, to_clob(''), '\"'||replace(replace(extract($(tableAlias).\"$(columnName)\", '/').getclobval(),'\\','\\\\'),'\"','\\\"')||'\"')" ;
        binaryColumnTemplate = blobColumnTemplate;
        triggerConcatCharacter = "||" ;
        newTriggerValue = ":new" ;
        oldTriggerValue = ":old" ;
        oldColumnPrefix = "" ;
        newColumnPrefix = "" ;

        sqlTemplates = new HashMap<String,String>();
        sqlTemplates.put("insertTriggerTemplate",
"create or replace trigger $(triggerName)\n" +
"after insert on $(schemaName)$(tableName)\n" +
"for each row\n" +
"begin\n" +
"    $(custom_before_insert_text)\n" +
"    if $(syncOnInsertCondition) and $(syncOnIncomingBatchCondition) then\n" +
"        begin\n" +
"            insert into $(defaultSchema)$(prefixName)_data\n" +
"            (table_name, event_type, trigger_hist_id, row_data, channel_id,\n" +
"            transaction_id, source_node_id, external_data, create_time)\n" +
"            values(\n" +
"            '$(targetTableName)',\n" +
"            'I',\n" +
"            $(triggerHistoryId),\n" +
"            $(oracleToClob)$(columns),\n" +
"            $(channelExpression),\n" +
"            $(txIdExpression),\n" +
"            $(prefixName)_pkg.disable_node_id,\n" +
"            $(externalSelect),\n" +
"            " + getCreateTimeExpression(symmetricDialect) + "\n" +
"            );\n" +
"        exception\n" +
"        when others then\n" +
"            insert into $(defaultSchema)$(prefixName)_data\n" +
"            (table_name, event_type, trigger_hist_id, row_data, channel_id,\n" +
"            transaction_id, source_node_id, external_data, create_time)\n" +
"            values(\n" +
"            '$(targetTableName)',\n" +
"            'I',\n" +
"            $(triggerHistoryId),\n" +
"            $(oracleToClobAlways)$(columnsClobAlways),\n" +
"            $(channelExpression),\n" +
"            $(txIdExpression),\n" +
"            $(prefixName)_pkg.disable_node_id,\n" +
"            $(externalSelect),\n" +
"            " + getCreateTimeExpression(symmetricDialect) + "\n" +
"            );\n" +
"        end;\n" +
"    end if;\n" +
"    $(custom_on_insert_text)\n" +
"end;\n");

        sqlTemplates.put("insertReloadTriggerTemplate",
"create or replace trigger $(triggerName)\n" +
"after insert on $(schemaName)$(tableName)\n" +
"for each row\n" +
"begin\n" +
"    $(custom_before_insert_text)\n" +
"    if $(syncOnInsertCondition) and $(syncOnIncomingBatchCondition) then\n" +
"        insert into $(defaultSchema)$(prefixName)_data\n" +
"        (table_name, event_type, trigger_hist_id, pk_data, channel_id,\n" +
"        transaction_id, source_node_id, external_data, create_time)\n" +
"        values(\n" +
"        '$(targetTableName)',\n" +
"        'R',\n" +
"        $(triggerHistoryId),\n" +
"        $(newKeys),\n" +
"        $(channelExpression),\n" +
"        $(txIdExpression),\n" +
"        $(prefixName)_pkg.disable_node_id,\n" +
"        $(externalSelect),\n" +
"        " + getCreateTimeExpression(symmetricDialect) + "\n" +
"        );\n" +
"    end if;\n" +
"    $(custom_on_insert_text)\n" +
"end;\n");

        sqlTemplates.put("updateTriggerTemplate",
"create or replace trigger $(triggerName) after update on $(schemaName)$(tableName)\n" +
"for each row\n" +
"begin\n" +
"    $(custom_before_update_text)\n" +
"    if $(syncOnUpdateCondition) and $(syncOnIncomingBatchCondition) then\n" +
"        declare\n" +
"            var_row_data $(oracleLobType);\n" +
"            var_old_data $(oracleLobType);\n" +
"        begin\n" +
"            select $(oracleToClob)$(columns) into var_row_data from dual;\n" +
"            select $(oracleToClob)$(oldColumns) into var_old_data from dual;\n" +
"            if $(dataHasChangedCondition) then\n" +
"                insert into $(defaultSchema)$(prefixName)_data\n" +
"                (table_name, event_type, trigger_hist_id, pk_data, row_data, old_data, channel_id, transaction_id, source_node_id, external_data, create_time)\n" +
"                values(\n" +
"                '$(targetTableName)',\n" +
"                'U',\n" +
"                $(triggerHistoryId),\n" +
"                $(oldKeys),\n" +
"                var_row_data,\n" +
"                var_old_data,\n" +
"                $(channelExpression),\n" +
"                $(txIdExpression),\n" +
"                $(prefixName)_pkg.disable_node_id,\n" +
"                $(externalSelect),\n" +
"                " + getCreateTimeExpression(symmetricDialect) + "\n" +
"                );\n" +
"            end if;\n" +
"        exception\n" +
"        when others then\n" +
"            declare\n" +
"                var_row_data $(oracleLobTypeClobAlways);\n" +
"                var_old_data $(oracleLobTypeClobAlways);\n" +
"            begin\n" +
"                select $(oracleToClobAlways)$(columnsClobAlways) into var_row_data from dual;\n" +
"                select $(oracleToClobAlways)$(oldColumnsClobAlways) into var_old_data from dual;\n" +
"                if $(dataHasChangedConditionClobAlways) then\n" +
"                    insert into $(defaultSchema)$(prefixName)_data\n" +
"                    (table_name, event_type, trigger_hist_id, pk_data, row_data, old_data, channel_id, transaction_id, source_node_id, external_data, create_time)\n" +
"                    values(\n" +
"                    '$(targetTableName)',\n" +
"                    'U',\n" +
"                    $(triggerHistoryId),\n" +
"                    $(oldKeys),\n" +
"                    var_row_data,\n" +
"                    var_old_data,\n" +
"                    $(channelExpression),\n" +
"                    $(txIdExpression),\n" +
"                    $(prefixName)_pkg.disable_node_id,\n" +
"                    $(externalSelect),\n" +
"                    " + getCreateTimeExpression(symmetricDialect) + "\n" +
"                    );\n" +
"                end if;\n" +
"            end;\n" +
"        end;\n" +
"    end if;\n" +
"    $(custom_on_update_text)\n" +
"end;\n");

        sqlTemplates.put("updateReloadTriggerTemplate",
"create or replace trigger $(triggerName) after update on $(schemaName)$(tableName)\n" +
"for each row\n" +
"begin\n" +
"    $(custom_before_update_text)\n" +
"    if $(syncOnUpdateCondition) and $(syncOnIncomingBatchCondition) then\n" +
"        insert into $(defaultSchema)$(prefixName)_data\n" +
"        (table_name, event_type, trigger_hist_id, pk_data, channel_id, transaction_id, source_node_id, external_data, create_time)\n" +
"        values(\n" +
"        '$(targetTableName)',\n" +
"        'R',\n" +
"        $(triggerHistoryId),\n" +
"        $(oldKeys),\n" +
"        $(channelExpression),\n" +
"        $(txIdExpression),\n" +
"        $(prefixName)_pkg.disable_node_id,\n" +
"        $(externalSelect),\n" +
"        " + getCreateTimeExpression(symmetricDialect) + "\n" +
"        );\n" +
"    end if;\n" +
"    $(custom_on_update_text)\n" +
"end;\n");

        sqlTemplates.put("deleteTriggerTemplate",
"create or replace trigger  $(triggerName) after delete on $(schemaName)$(tableName)\n" +
"for each row\n" +
"begin\n" +
"    $(custom_before_delete_text)\n" +
"    if $(syncOnDeleteCondition) and $(syncOnIncomingBatchCondition) then\n" +
"        begin\n" +
"            insert into $(defaultSchema)$(prefixName)_data\n" +
"            (table_name, event_type, trigger_hist_id, pk_data, old_data, channel_id, transaction_id, source_node_id, external_data, create_time)\n" +
"            values(\n" +
"            '$(targetTableName)',\n" +
"            'D',\n" +
"            $(triggerHistoryId),\n" +
"            $(oldKeys),\n" +
"            $(oracleToClob)$(oldColumns),\n" +
"            $(channelExpression),\n" +
"            $(txIdExpression),\n" +
"            $(prefixName)_pkg.disable_node_id,\n" +
"            $(externalSelect),\n" +
"            " + getCreateTimeExpression(symmetricDialect) + "\n" +
"            );\n" +
"        exception\n" +
"        when others then\n" +
"            insert into $(defaultSchema)$(prefixName)_data\n" +
"            (table_name, event_type, trigger_hist_id, pk_data, old_data, channel_id, transaction_id, source_node_id, external_data, create_time)\n" +
"            values(\n" +
"            '$(targetTableName)',\n" +
"            'D',\n" +
"            $(triggerHistoryId),\n" +
"            $(oldKeys),\n" +
"            $(oracleToClobAlways)$(oldColumnsClobAlways),\n" +
"            $(channelExpression),\n" +
"            $(txIdExpression),\n" +
"            $(prefixName)_pkg.disable_node_id,\n" +
"            $(externalSelect),\n" +
"            " + getCreateTimeExpression(symmetricDialect) + "\n" +
"            );\n" +
"        end;\n" +
"    end if;\n" +
"    $(custom_on_delete_text)\n" +
"end;\n");

        sqlTemplates.put("initialLoadSqlTemplate" ,
"select $(oracleQueryHint) $(oracleToClob)$(columns) from $(schemaName)$(tableName) t where $(whereClause)");
    }

    protected String getNumberConversionString() {
        if (symmetricDialect.getParameterService().is(ParameterConstants.DBDIALECT_ORACLE_TEMPLATE_NUMBER_TEXT_MINIMUM)) {
            return "to_char($(tableAlias).\"$(columnName)\", 'TM')";
        } else {
            return "cast($(tableAlias).\"$(columnName)\" as number(" + symmetricDialect.getTemplateNumberPrecisionSpec() + "))";
        }
    }
    protected String getCreateTimeExpression(ISymmetricDialect symmetricDialect) {
        String timezone = symmetricDialect.getParameterService().getString(ParameterConstants.DATA_CREATE_TIME_TIMEZONE);
        if (StringUtils.isEmpty(timezone)) {
            return "CURRENT_TIMESTAMP";
        } else {
            return String.format("CURRENT_TIMESTAMP AT TIME ZONE '%s'", timezone);
        }
    }

    protected String toClobExpression(Table table) {
        if (symmetricDialect.getParameterService().is(ParameterConstants.DBDIALECT_ORACLE_USE_NTYPES_FOR_SYNC)) {
            return "to_nclob('')||";
        } else {
            return "to_clob('')||";
        }
    }

}
