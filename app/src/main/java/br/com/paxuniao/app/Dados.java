package br.com.paxuniao.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

public class Dados {

    private static final String TAG = "DADOS";
    public static SQLiteDatabase BancoDados = null;
    public static SQLiteDatabase BancoLog = null;

    Context ctx;
    public static int DBVersion = 0 ;

    private DadosJson dadosJson;

    public DadosJson getJsonHelper() {
        if (dadosJson == null) {
            dadosJson = new DadosJson();
        }
        return dadosJson;
    }

    public void open(Context nctx){
        Log.i(TAG, "Dados abrindo...");
        ctx=nctx;
        boolean inserir_dados=false;
        java.io.File dbFile = ctx.getDatabasePath("dados.db");
        if (!dbFile.exists()) {
            inserir_dados=true;
        }

        BancoDados = ctx.openOrCreateDatabase("dados.db",  Context.MODE_PRIVATE,	null);
        BancoLog = ctx.openOrCreateDatabase("log.db",Context.MODE_PRIVATE, null);

        String sql = "CREATE TABLE IF NOT EXISTS TBSYS (S_KEY VARCHAR(20) PRIMARY KEY, S_TIPO INTEGER, S_TXT_60 VARCHAR(60));";
        BancoDados.execSQL(sql);

        String sql4 = "CREATE TABLE IF NOT EXISTS TBLOG2(LOG_TXT VARCHAR(1000))";
        BancoLog.execSQL(sql4);

        String sql5 = "CREATE TABLE IF NOT EXISTS TBERRO( ERR_SEQ INTEGER PRIMARY KEY AUTOINCREMENT, ERR_DTHR VARCHAR(25), ERR_TXT VARCHAR(1000), ERR_DRHRMAIL VARCHAR(25) ) ";
        BancoLog.execSQL(sql5);

        update_db();

        if (inserir_dados) {
            inserirDadosExemplo();
        }
    }

    public int getDBVersion(){
        return DBVersion;
    }

    public void close(){
        if (BancoDados != null) BancoDados.close();
    }

    @SuppressLint("Range")
    private void update_db(){
        Cursor cursor = BancoDados.query("TBSYS", new String[] { "S_KEY", "S_TIPO"  }, "S_KEY='ver'", null, null, null, null);
        int mStep = 0;
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            mStep = cursor.getInt(cursor.getColumnIndex("S_TIPO"));
        }
        cursor.close();

        //-- 0. TB_LOGIN
        mStep = update_db_exec(mStep, 0, "CREATE TABLE IF NOT EXISTS TB_LOGIN (LOGIN_SEQ INTEGER PRIMARY KEY AUTOINCREMENT, LOGIN_CPF TEXT NOT NULL UNIQUE, LOGIN_SENHA TEXT NOT NULL);");

        // -- 1. TB_CLI
        mStep = update_db_exec(mStep, 1, "CREATE TABLE IF NOT EXISTS TB_CLI (CLI_LOGIN_SEQ INTEGER NOT NULL, CLI_CODIGO TEXT NOT NULL, CLI_NOME TEXT NOT NULL, CLI_ENDERECO TEXT NOT NULL, CLI_ENDERECON TEXT NOT NULL, CLI_CIDADE TEXT NOT NULL, CLI_UF TEXT NOT NULL, CLI_CEP TEXT NOT NULL, CLI_TIPOPLANO INTEGER NOT NULL, CLI_CLI_DATA_TRANS TEXT, CLI_SITUACAO TEXT, CLI_DATAPLANO TEXT, CLI_CPF TEXT, FOREIGN KEY (CLI_LOGIN_SEQ) REFERENCES TB_LOGIN(LOGIN_SEQ));");

        //-- 2. TB_DEPENDENTES
        mStep = update_db_exec(mStep, 2, "CREATE TABLE IF NOT EXISTS TB_DEPENDENTES (DEP_SEQ INTEGER PRIMARY KEY AUTOINCREMENT, DEP_CLI_CODIGO TEXT NOT NULL, DEP_NOME TEXT NOT NULL, DEP_GRAU_PARENTESCO TEXT, FOREIGN KEY (DEP_CLI_CODIGO) REFERENCES TB_CLI(CLI_CODIGO));");

        //-- 3. TB_CX
        mStep = update_db_exec(mStep, 3, "CREATE TABLE IF NOT EXISTS TB_CX (CX_SEQ INTEGER PRIMARY KEY AUTOINCREMENT, CX_CLI_CODIGO TEXT NOT NULL, CX_NUMERO INTEGER, CX_VENCIMENTO TEXT, CX_VALOR REAL, CX_STATUS TEXT, CX_DT_PGTO TEXT, CX_MES TEXT, CX_ANO INTEGER, CX_CODIGO_BARRAS TEXT, FOREIGN KEY (CX_CLI_CODIGO) REFERENCES TB_CLI(CLI_CODIGO));");

        //-- 4. TB_CONVENIADOS
        mStep = update_db_exec(mStep, 4, "CREATE TABLE IF NOT EXISTS TB_CONVENIADOS (CVN_SEQ INTEGER PRIMARY KEY AUTOINCREMENT, CVN_NOME TEXT NOT NULL, CVN_DESCRICAO TEXT, CVN_CATEGORIA TEXT, CVN_DESCONTO TEXT, CVN_ICONE_HASH TEXT);");

        //-- Migrações para garantir colunas em bancos antigos (Ex: versão 141+)
        mStep = update_db_exec_fix(mStep, 141, "ALTER TABLE TB_CLI ADD COLUMN CLI_DATAPLANO TEXT;");
        mStep = update_db_exec_fix(mStep, 142, "ALTER TABLE TB_CLI ADD COLUMN CLI_CPF TEXT;");

        DBVersion = mStep;
        BancoDados.execSQL("INSERT OR REPLACE INTO TBSYS (S_KEY, S_TIPO) VALUES ('ver'," + mStep + ");");
    }

    private int update_db_exec(int mStep, int mStepTest, String mSql){
        if (mStepTest == mStep){
            try {
                BancoDados.execSQL(mSql);
            } catch (Exception e){ 
                Log.e(TAG, "Erro Step " + mStepTest + ": " + e.getMessage());
            }
            mStep++;
        }
        return mStep;
    }

    private int update_db_exec_fix(int mStep, int mStepTest, String mSql){
        // Força a execução se o Step atual for menor ou igual ao Test (caso o Step tenha pulado)
        // Ou se estiver exatamente no Step. Aqui usamos uma lógica simples de "se mStep for menor, executa e pula"
        if (mStep <= mStepTest){
            try {
                BancoDados.execSQL(mSql);
            } catch (Exception e){ 
                // Se a coluna já existir, ele cai aqui e ignora
                Log.i(TAG, "Migração ignorada ou já aplicada: " + mSql);
            }
            mStep = mStepTest + 1;
        }
        return mStep;
    }

    public void putString(String m_Key, String txt){
        BancoDados.execSQL("INSERT OR REPLACE INTO TBSYS (S_KEY, S_TXT_60) VALUES ('"+m_Key+"','" + txt + "');");
    }

    @SuppressLint("Range")
    public String getString(String m_Key){
        String txt ="";
        Cursor cursor = BancoDados.query("TBSYS", new String[] { "S_TXT_60"  }, "S_KEY='"+m_Key+"'", null, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            txt = cursor.getString(cursor.getColumnIndex("S_TXT_60"));
        }
        cursor.close();
        return txt;
    }

    @SuppressLint("Range")
    public String obterJsonGenerico(String sql) {
        JSONArray jsonArray = new JSONArray();
        try {
            Cursor cursor = BancoDados.rawQuery(sql, null);
            if (cursor.moveToFirst()) {
                do {
                    JSONObject obj = new JSONObject();
                    for (int i = 0; i < cursor.getColumnCount(); i++) {
                        String colName = cursor.getColumnName(i);
                        switch (cursor.getType(i)) {
                            case Cursor.FIELD_TYPE_INTEGER: obj.put(colName, cursor.getLong(i)); break;
                            case Cursor.FIELD_TYPE_FLOAT: obj.put(colName, cursor.getDouble(i)); break;
                            case Cursor.FIELD_TYPE_STRING: obj.put(colName, cursor.getString(i)); break;
                            case Cursor.FIELD_TYPE_NULL: obj.put(colName, JSONObject.NULL); break;
                            default: obj.put(colName, cursor.getString(i)); break;
                        }
                    }
                    jsonArray.put(obj);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) { Log.e(TAG, "Erro JSON: " + e.getMessage()); }
        return jsonArray.toString();
    }

    public String getJsonCliente(String cliCodigo) {
        return obterJsonGenerico("SELECT * FROM TB_CLI WHERE CLI_CODIGO = '" + cliCodigo + "'");
    }

    public String getJsonPrimeiroCliente() {
        return obterJsonGenerico("SELECT * FROM TB_CLI ORDER BY CLI_CODIGO ASC LIMIT 1");
    }

    public String getJsonDependentes(String cliCodigo) {
        return obterJsonGenerico("SELECT * FROM TB_DEPENDENTES WHERE DEP_CLI_CODIGO = '" + cliCodigo + "'");
    }

    public String getJsonParcelas(String cliCodigo) {
        return obterJsonGenerico("SELECT * FROM TB_CX WHERE CX_CLI_CODIGO = '" + cliCodigo + "' ORDER BY CX_NUMERO DESC");
    }

    public String getJsonConveniados() {
        return obterJsonGenerico("SELECT CVN_NOME AS CONV_NOME, CVN_DESCRICAO AS CONV_ENDERECO, CVN_CATEGORIA AS CONV_CATEGORIA, CVN_DESCONTO AS CONV_DESCONTO, CVN_ICONE_HASH AS CONV_ICONE FROM TB_CONVENIADOS ORDER BY CVN_NOME ASC");
    }

    public void inserirDadosExemplo() {
        try {
            BancoDados.beginTransaction();
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_LOGIN (LOGIN_SEQ, LOGIN_CPF, LOGIN_SENHA) VALUES (1, '12345678901', 'senha123');");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CLI (CLI_LOGIN_SEQ, CLI_CODIGO, CLI_NOME, CLI_ENDERECO, CLI_ENDERECON, CLI_CIDADE, CLI_UF, CLI_CEP, CLI_TIPOPLANO, CLI_SITUACAO, CLI_DATAPLANO, CLI_CPF) VALUES (1, '0055-A1', 'RAFAEL (TESTE)', 'Rua Teste, 123', 'Centro', 'TERESINA', 'PI', '64000-000', 1, 'ATIVO', '20/03/2015', '64293840397')");
            BancoDados.setTransactionSuccessful();
            // Versão após inserção de dados de exemplo
            BancoDados.execSQL("INSERT OR REPLACE INTO TBSYS (S_KEY, S_TIPO) VALUES ('ver', 143);");
        } catch (Exception e) { Log.e(TAG, "Erro exemplo", e); } finally { BancoDados.endTransaction(); }
    }

    public class DadosJson {
        public String getContratosJson() {
            String codigoContrato = getString("CONTRATO_ATIVO");

            // 1. Definimos a lógica do CASE SQL baseada no Flavor
            String sqlPlano;

            if ("unipax".equals(BuildConfig.FLAVOR)) {
                sqlPlano = "CASE CLI_TIPOPLANO " +
                        "  WHEN 1 THEN 'SAFIRA' " +
                        "  WHEN 2 THEN 'RUBI' " +
                        "  WHEN 3 THEN 'ESMERALDA' " +
                        "  WHEN 4 THEN 'DIAMANTE' " +
                        "  ELSE 'PLANO PADRÃO' " +
                        "END";
            } else {
                sqlPlano = "CASE CLI_TIPOPLANO " +
                        "  WHEN 1 THEN 'SIMPLES' " +
                        "  WHEN 2 THEN 'LUXO' " +
                        "  WHEN 3 THEN 'SUPER LUXO' " +
                        "  WHEN 4 THEN 'SUPER LUXO ESPECIAL' " +
                        "  ELSE 'PLANO PADRÃO' " +
                        "END";
            }

            // 2. Montamos a query completa usando a variável sqlPlano
            String sql = "SELECT CLI_CODIGO, CLI_NOME, CLI_SITUACAO, CLI_DATAPLANO, CLI_CPF, " +
                    "CASE WHEN CLI_CODIGO = '" + codigoContrato + "' THEN -1 ELSE 0 END AS CLI_SELECIONADO, " +
                    sqlPlano + " AS CLI_PLANO " + // Aqui entra a conversão
                    "FROM TB_CLI " +
                    "ORDER BY CLI_NOME ASC";
            return obterJsonGenerico(sql);
        }
    }

    public void apagacliantes() {
        BancoDados.execSQL("DELETE FROM TB_CLI");
    }

    public void sincronizarClienteApi(int loginSeq, JSONObject cliJson) {
        try {
            BancoDados.beginTransaction();
            String sql = "INSERT OR REPLACE INTO TB_CLI " +
                    "(CLI_LOGIN_SEQ, CLI_CODIGO, CLI_NOME, CLI_ENDERECO, CLI_ENDERECON, CLI_CIDADE, CLI_UF, CLI_CEP, CLI_TIPOPLANO, CLI_CLI_DATA_TRANS, CLI_SITUACAO, CLI_DATAPLANO, CLI_CPF, CLI_TIPOPLANO) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            BancoDados.execSQL(sql, new Object[]{
                    loginSeq,
                    cliJson.optString("CLI_CODIGO", ""),
                    cliJson.optString("CLI_NOME", ""),
                    cliJson.optString("CLI_ENDERECO", ""),
                    cliJson.optString("CLI_ENDERECON", ""),
                    cliJson.optString("CLI_CIDADE", ""),
                    cliJson.optString("CLI_UF", ""),
                    cliJson.optString("CLI_CEP", ""),
                    cliJson.optInt("CLI_TIPOPLANO", 1),
                    cliJson.optString("CLI_CLI_DATA_TRANS", ""),
                    cliJson.optString("CLI_SITUACAO", "ATIVO"),
                    cliJson.optString("CLI_DATAPLANO", ""),
                    cliJson.optString("CLI_CPF", ""),
                    cliJson.optString("CLI_TIPOPLANO", "")
            });
            BancoDados.setTransactionSuccessful();
        } catch (Exception e) { Log.e(TAG, "Erro sync cliente", e); } finally { BancoDados.endTransaction(); }
    }

    public void sincronizarDependentesApi(String cliCodigo, JSONArray dependentesArray) {
        try {
            BancoDados.beginTransaction();
            BancoDados.execSQL("DELETE FROM TB_DEPENDENTES WHERE DEP_CLI_CODIGO = ?", new Object[]{cliCodigo});
            String sql = "INSERT INTO TB_DEPENDENTES (DEP_CLI_CODIGO, DEP_NOME, DEP_GRAU_PARENTESCO) VALUES (?, ?, ?)";
            for (int i = 0; i < dependentesArray.length(); i++) {
                JSONObject dep = dependentesArray.getJSONObject(i);
                BancoDados.execSQL(sql, new Object[]{cliCodigo, dep.optString("DEP_NOME", ""), dep.optString("DEP_GRAU_PARENTESCO", "")});
            }
            BancoDados.setTransactionSuccessful();
        } catch (Exception e) { Log.e(TAG, "Erro sync dep", e); } finally { BancoDados.endTransaction(); }
    }

    public void sincronizarParcelasApi(String cliCodigo, JSONArray parcelasArray) {
        try {
            BancoDados.beginTransaction();
            BancoDados.execSQL("DELETE FROM TB_CX WHERE CX_CLI_CODIGO = ?", new Object[]{cliCodigo});
            String sql = "INSERT INTO TB_CX (CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            for (int i = 0; i < parcelasArray.length(); i++) {
                JSONObject parc = parcelasArray.getJSONObject(i);
                BancoDados.execSQL(sql, new Object[]{cliCodigo, parc.optInt("CX_NUMERO", 0), parc.optString("CX_VENCIMENTO", ""), parc.optDouble("CX_VALOR", 0.0), parc.optString("CX_STATUS", "aberto"), parc.optString("CX_DT_PGTO", null), parc.optString("CX_MES", ""), parc.optInt("CX_ANO", 0), parc.optString("CX_CODIGO_BARRAS", "")});
            }
            BancoDados.setTransactionSuccessful();
        } catch (Exception e) { Log.e(TAG, "Erro sync parcelas", e); } finally { BancoDados.endTransaction(); }
    }

    public void sincronizarConveniadosApi_Apaga() {
        try {
            BancoDados.beginTransaction();
            BancoDados.execSQL("DELETE FROM TB_CONVENIADOS");
            BancoDados.setTransactionSuccessful();
        } catch (Exception e) { Log.e(TAG, "Erro sync conv", e); } finally { BancoDados.endTransaction(); }
    }

    public void sincronizarConveniadosApi(JSONArray conveniadosArray) {
        try {
            BancoDados.beginTransaction();
            BancoDados.execSQL("DELETE FROM TB_CONVENIADOS");
            String sql = "INSERT INTO TB_CONVENIADOS (CVN_NOME, CVN_DESCRICAO, CVN_CATEGORIA, CVN_DESCONTO, CVN_ICONE_HASH) VALUES (?, ?, ?, ?, ?)";
            for (int i = 0; i < conveniadosArray.length(); i++) {
                JSONObject conv = conveniadosArray.getJSONObject(i);
                BancoDados.execSQL(sql, new Object[]{conv.optString("CONV_NOME", ""), conv.optString("CONV_ENDERECO", ""), conv.optString("CONV_CATEGORIA", "outros"), conv.optString("CONV_DESCONTO", ""), conv.optString("CONV_ICONE", "fa-store")});
            }
            BancoDados.setTransactionSuccessful();
        } catch (Exception e) { Log.e(TAG, "Erro sync conv", e); } finally { BancoDados.endTransaction(); }
    }
}
