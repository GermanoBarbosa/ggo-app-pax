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

    public void open(Context nctx){
        Log.i(TAG, "Dados abrindo...");
        int mStep=0;
        ctx=nctx;
        BancoDados = ctx.openOrCreateDatabase("dados.db",  Context.MODE_PRIVATE,	null);
        BancoLog = ctx.openOrCreateDatabase("log.db",Context.MODE_PRIVATE, null);

        //BancoDados = ctx.openOrCreateDatabase("/data/data/br.com.praticonet/databases/dados.db",  Context.MODE_PRIVATE,	null);
        String sql = "CREATE TABLE IF NOT EXISTS TBSYS (S_KEY VARCHAR(20) PRIMARY KEY, S_TIPO INTEGER, S_TXT_60 VARCHAR(60));";
        BancoDados.execSQL(sql);

        String sql4 = "CREATE TABLE IF NOT EXISTS TBLOG2(LOG_TXT VARCHAR(1000))";
        BancoLog.execSQL(sql4);

        String sql5 = "CREATE TABLE IF NOT EXISTS TBERRO( ERR_SEQ INTEGER PRIMARY KEY AUTOINCREMENT, ERR_DTHR VARCHAR(25), ERR_TXT VARCHAR(1000), ERR_DRHRMAIL VARCHAR(25) ) ";
        BancoLog.execSQL(sql5);

        update_db();
    }

    public int getDBVersion(){
        return DBVersion;
    }

    public void run(){

    }

    public void close(){
        BancoDados.close();
    }

    @SuppressLint("Range")
    private void update_db(){
       String sql;
        sql="";
        Cursor cursor = BancoDados.query(
                "TBSYS",
                new String[] { "S_KEY", "S_TIPO"  }, "S_KEY='ver'", null, null, null, null);
        int NumeroRegistro = cursor.getCount();
        int mStep = 0;
        //Log.i("Banco Versão - NumeroRegistro",NumeroRegistro+"");
        Log.i("Banco Versão Registros",NumeroRegistro+"");
        if (NumeroRegistro > 0) {
            cursor.moveToFirst();
            //if (!cursor.isAfterLast()) {
            //cursor.getString(cursor.getColumnIndex("L_REF1")).equalsIgnoreCase( cursor.getString(cursor.getColumnIndex("L_REF2")))
            mStep = cursor.getInt(cursor.getColumnIndex("S_TIPO"));
            //}
        }
        cursor.close();
        Log.i("Banco Versão",mStep+"");

        //-- 1. Tabela de Logon
        sql=    "CREATE TABLE TB_LOGIN (\n" +
                "    LOGIN_SEQ INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    LOGIN_CPF TEXT NOT NULL UNIQUE," +
                "    LOGIN_SENHA TEXT NOT NULL" +
                ");";
        mStep = update_db_exec(mStep,0,sql);

        // -- 2. Tabela de Dados de Cliente
        sql=    "CREATE TABLE TB_CLI (" +
                "CLI_LOGIN_SEQ INTEGER NOT NULL," +
                "CLI_CODIGO TEXT NOT NULL," +
                "CLI_NOME  TEXT NOT NULL," +
                "CLI_ENDERECO TEXT NOT NULL," +
                "CLI_ENDERECON TEXT NOT NULL," +
                "CLI_CIDADE TEXT NOT NULL," +
                "CLI_UF TEXT NOT NULL," +
                "CLI_CEP  TEXT NOT NULL," +
                "CLI_TIPOPLANO INTEGER NOT NULL," +
                "CLI_CLI_DATA_TRANS TEXT," +
                "CLI_SITUACAO TEXT," +
                "FOREIGN KEY (CLI_LOGIN_SEQ) REFERENCES TB_LOGIN(LOGIN_SEQ)\n" +
                ");";
        mStep = update_db_exec(mStep,1,sql);

        //-- 3. Tabela de Dependentes (Relacionada ao Contrato)
        sql=  "CREATE TABLE TB_DEPENDENTES (" +
                "DEP_SEQ INTEGER PRIMARY KEY AUTOINCREMENT," +
                "DEP_CLI_CODIGO TEXT NOT NULL," +
                "DEP_NOME TEXT NOT NULL," +
                "DEP_GRAU_PARENTESCO TEXT," +
                "FOREIGN KEY (DEP_CLI_CODIGO) REFERENCES TB_CLI(CLI_CODIGO)" +
                ");";
        mStep = update_db_exec(mStep,2,sql);

        //-- 4. Tabela de Parcelas (Relacionada ao Contrato)
        sql=    "CREATE TABLE TB_CX (\n" +
                "    CX_SEQ INTEGER PRIMARY KEY AUTOINCREMENT, -- DM_INTEGER32\n" +
                "    CX_CLI_CODIGO TEXT NOT NULL,              -- Relacionamento com PAI: {PREFIXO}_{PK_PAI}\n" +
                "    CX_NUMERO INTEGER,                        -- DM_INTEGER32 (Ex: 26)\n" +
                "    CX_VENCIMENTO TEXT,                       -- DM_DT (YYYY-MM-DD)\n" +
                "    CX_VALOR REAL,                            -- DM_CURRENCY\n" +
                "    CX_STATUS TEXT,                           -- DMS_TEXTO_20 ('aberto', 'vencido', 'pago')\n" +
                "    CX_DT_PGTO TEXT,                          -- DM_DT (YYYY-MM-DD, nulo se não pago)\n" +
                "    CX_MES TEXT,                              -- DMS_TEXTO_20 (Ex: 'MAIO')\n" +
                "    CX_ANO INTEGER,                           -- DM_INTEGER16 (Ex: 2026)\n" +
                "    CX_CODIGO_BARRAS TEXT,                    -- DMS_TEXTO_100\n" +
                "    FOREIGN KEY (CX_CLI_CODIGO) REFERENCES TB_CLI(CLI_CODIGO)\n" +
                ");";
        mStep = update_db_exec(mStep,3,sql);

        //-- 5. Tabela de Conveniados do Clube Pax
        sql= "CREATE TABLE TB_CONVENIADOS (\n" +
                "    CVN_SEQ INTEGER PRIMARY KEY AUTOINCREMENT,  -- DM_INTEGER32\n" +
                "    CVN_NOME TEXT NOT NULL,                     -- DMS_NOME\n" +
                "    CVN_DESCRICAO TEXT,                         -- DMS_TEXTO_100\n" +
                "    CVN_CATEGORIA TEXT,                         -- DMS_TEXTO_30 ('saude', 'educacao', 'lazer')\n" +
                "    CVN_DESCONTO TEXT,                          -- DMS_TEXTO_30 (Ex: 'ATÉ 40% OFF')\n" +
                "    CVN_ICONE_HASH TEXT                         -- DMS_HASH (Apenas o hash da imagem)\n" +
                ");";
        mStep = update_db_exec(mStep,4,sql);




        mStep = update_db_exec(mStep, 141, "ALTER TABLE PIX_RETORNO ADD PIX_SEQ INTEGER NOT NULL DEFAULT 0");

        /* LOTE_STATUS
         *  0 = Não enviado
         *  1 = Enviado
         *  2 = Baixado
         */
        DBVersion=mStep;
        Log.i("Banco Versão",mStep+"");
        sql = "INSERT OR REPLACE INTO TBSYS (S_KEY, S_TIPO) VALUES ('ver'," + mStep + "); ";
        BancoDados.execSQL(sql);
    }

    private int update_db_exec( int mStep,int mStepTest,String mSql ){
        if (mStepTest==mStep){
            try {
                BancoDados.execSQL(mSql);
                mStep++;
            }catch (Exception e){
                e.printStackTrace();
                Log.i("LIOLIO",e.getMessage());
            }
            return mStep;
        } else {
            return mStep;
        }
    }

    public void putString(String m_Key, String txt){
        String sql = "INSERT OR REPLACE INTO TBSYS (S_KEY, S_TXT_60) VALUES ('"+m_Key+"','" + txt + "'); ";
        BancoDados.execSQL(sql);
    }

    @SuppressLint("Range")
    public static String getString(String m_Key){
        String sql = "";
        String txt ="";
        Cursor cursor = BancoDados.query(
                "TBSYS",
                new String[] { "S_KEY", "S_TIPO", "S_TXT_60"  }, "S_KEY='"+m_Key+"'", null, null, null, null);
        int NumeroRegistro = cursor.getCount();
        if (NumeroRegistro > 0) {
            cursor.moveToFirst();
            txt = cursor.getString(cursor.getColumnIndex("S_TXT_60"));
        }
        cursor.close();
        return txt;
    }

    public static int getLastID(String m_Table) {
        final String MY_QUERY = "SELECT last_insert_rowid() FROM "+ m_Table;
        Cursor cur = BancoDados.rawQuery(MY_QUERY, null);
        cur.moveToFirst();
        int ID = cur.getInt(0);
        cur.close();
        return ID;
    }

    public int regCount(String m_sql) {
        Cursor cur = BancoDados.rawQuery(m_sql, null);
        int ID = cur.getCount();
        cur.close();
        return ID;
    }

    @SuppressLint("Range")
    public String executeRetString(String m_sql, String m_field) {
//        String ret ="";
//        Log.i("ExecuteRetString",m_sql);
//        try{
//            Cursor cur = BancoDados.rawQuery(m_sql, null);
//            if (cur.getCount() > 0){
//                ret =cur.getString(cur.getColumnIndex(m_field));
//            }
//            cur.close();
//        } catch (Exception ex) { }
//        return ret;

        String ret ="";
        try{
            Cursor cur = BancoDados.rawQuery(m_sql, null);
            if (cur.getCount() > 0){
                cur.moveToFirst();
                ret =cur.getString(cur.getColumnIndex(m_field));
            }
            cur.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return ret;
    }

    /**
     * Método genérico para executar um SELECT e retornar um JSONArray em formato de String.
     * Ideal para ser consumido pelo JavascriptInterface no WebView.
     */
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
                            case Cursor.FIELD_TYPE_INTEGER:
                                obj.put(colName, cursor.getLong(i));
                                break;
                            case Cursor.FIELD_TYPE_FLOAT:
                                obj.put(colName, cursor.getDouble(i));
                                break;
                            case Cursor.FIELD_TYPE_STRING:
                                obj.put(colName, cursor.getString(i));
                                break;
                            case Cursor.FIELD_TYPE_NULL:
                                obj.put(colName, JSONObject.NULL);
                                break;
                            default:
                                obj.put(colName, cursor.getString(i));
                                break;
                        }
                    }
                    jsonArray.put(obj);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao converter Cursor para JSON: " + e.getMessage());
        }
        return jsonArray.toString();
    }

    // --- Métodos Práticos Mapeando suas Tabelas ---

    public String getJsonCliente(String cliCodigo) {
        String sql = "SELECT * FROM TB_CLI WHERE CLI_CODIGO = '" + cliCodigo + "'";
        return obterJsonGenerico(sql);
    }

    public String getJsonDependentes(String cliCodigo) {
        String sql = "SELECT * FROM TB_DEPENDENTES WHERE DEP_CLI_CODIGO = '" + cliCodigo + "'";
        return obterJsonGenerico(sql);
    }

    public String getJsonParcelas(String cliCodigo) {
        // Busca o financeiro do cliente ordenado pelo vencimento
        String sql = "SELECT * FROM TB_CX WHERE CX_CLI_CODIGO = '" + cliCodigo + "' ORDER BY CX_VENCIMENTO DESC";
        return obterJsonGenerico(sql);
    }

    public String getJsonConveniados() {
        // Busca todos os conveniados ordenados por nome
        String sql = "SELECT * FROM TB_CONVENIADOS ORDER BY CVN_NOME ASC";
        return obterJsonGenerico(sql);
    }

}

