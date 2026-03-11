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

    // Instância da nossa nova classe geradora de JSON
    private DadosJson dadosJson;

    // Método para acessar os geradores de JSON
    public DadosJson getJsonHelper() {
        if (dadosJson == null) {
            dadosJson = new DadosJson();
        }
        return dadosJson;
    }

    public void open(Context nctx){
        Log.i(TAG, "Dados abrindo...");
        int mStep=0;
        ctx=nctx;
        boolean inserir_dados=false;
        // TESTE SE O BANCO EXISTE
        java.io.File dbFile = ctx.getDatabasePath("dados.db");
        if (!dbFile.exists()) {
            inserir_dados=true;
        } else {
            Log.i(TAG, "O banco de dados já existe.");
        }

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

        if (inserir_dados) {
            inserirDadosExemplo();
        }
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
        String sql=    "CREATE TABLE TB_LOGIN (" +
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
                "FOREIGN KEY (CLI_LOGIN_SEQ) REFERENCES TB_LOGIN(LOGIN_SEQ)" +
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
        sql=    "CREATE TABLE TB_CX (" +
                "    CX_SEQ INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "    CX_CLI_CODIGO TEXT NOT NULL," +
                "    CX_NUMERO INTEGER," +
                "    CX_VENCIMENTO TEXT," +
                "    CX_VALOR REAL," +
                "    CX_STATUS TEXT," +
                "    CX_DT_PGTO TEXT," +
                "    CX_MES TEXT," +
                "    CX_ANO INTEGER," +
                "    CX_CODIGO_BARRAS TEXT," +
                "    FOREIGN KEY (CX_CLI_CODIGO) REFERENCES TB_CLI(CLI_CODIGO)" +
                ");";
        mStep = update_db_exec(mStep,3,sql);

        //-- 5. Tabela de Conveniados do Clube Pax
        sql= "CREATE TABLE TB_CONVENIADOS (" +
                "    CVN_SEQ INTEGER PRIMARY KEY AUTOINCREMENT,"+
                "    CVN_NOME TEXT NOT NULL," +
                "    CVN_DESCRICAO TEXT," +
                "    CVN_CATEGORIA TEXT," +
                "    CVN_DESCONTO TEXT," +
                "    CVN_ICONE_HASH TEXT" +
                ");";
        mStep = update_db_exec(mStep,4,sql);




        //mStep = update_db_exec(mStep, 141, "ALTER TABLE PIX_RETORNO ADD PIX_SEQ INTEGER NOT NULL DEFAULT 0");

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
    public String getString(String m_Key){
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

    // Substitua a função getJsonConveniados existente por esta:
    public String getJsonConveniados() {
        // Selecionamos do banco e já renomeamos (AS) para os nomes que o HTML espera ler
        String sql = "SELECT " +
                "CVN_NOME AS CONV_NOME, " +
                "CVN_DESCRICAO AS CONV_ENDERECO, " +
                "CVN_CATEGORIA AS CONV_CATEGORIA, " +
                "CVN_DESCONTO AS CONV_DESCONTO, " +
                "CVN_ICONE_HASH AS CONV_ICONE " +
                "FROM TB_CONVENIADOS ORDER BY CVN_NOME ASC";

        return obterJsonGenerico(sql);
    }

    public void inserirDadosExemplo() {
        try {
            BancoDados.beginTransaction();

            // Inserir login
            BancoDados.execSQL("INSERT OR REPLACE INTO TBSYS (S_KEY, S_TIPO, S_TXT_60) VALUES ('ver', 141, 'Versão 1.4.0');");

            // Inserir login de exemplo
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_LOGIN (LOGIN_SEQ, LOGIN_CPF, LOGIN_SENHA) VALUES (1, '12345678901', 'senha123');");

            // Inserir dados do cliente principal (Ivoneide)
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CLI (CLI_LOGIN_SEQ, CLI_CODIGO, CLI_NOME, CLI_ENDERECO, CLI_ENDERECON, CLI_CIDADE, CLI_UF, CLI_CEP, CLI_TIPOPLANO, CLI_CLI_DATA_TRANS, CLI_SITUACAO) " +
                    "VALUES (1, '0044-A1', 'IVONEIDE S. NAVA ARAUJO', 'Rua Miguel Atta, 87', 'Centro', 'TERESINA', 'PI', '64000-000', 1, '20/03/2015', 'ATIVO')" );


            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CLI (CLI_LOGIN_SEQ, CLI_CODIGO, CLI_NOME, CLI_ENDERECO, CLI_ENDERECON, CLI_CIDADE, CLI_UF, CLI_CEP, CLI_TIPOPLANO, CLI_CLI_DATA_TRANS, CLI_SITUACAO) " +
                    "VALUES (1, '1111-A1', 'IVONEIDE S. NAVA ARAUJO', 'Rua Miguel Atta, 87', 'Centro', 'TERESINA', 'PI', '64000-000', 1, '20/03/2015', 'ATIVO')" );


            // Inserir dependentes
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_DEPENDENTES (DEP_SEQ, DEP_CLI_CODIGO, DEP_NOME, DEP_GRAU_PARENTESCO) VALUES (1, '0044-A1', 'João Pedro Nava Araujo', 'FILHO')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_DEPENDENTES (DEP_SEQ, DEP_CLI_CODIGO, DEP_NOME, DEP_GRAU_PARENTESCO) VALUES (2, '0055-A1', 'Maria Clara Nava Araujo', 'FILHA'),(3, '0055-A1', 'Antonio Carlos Araujo', 'CONJUGE');");

            // Inserir parcelas (financeiro)
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CX (CX_SEQ, CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) VALUES (1, '0055-A1', 26, '2026-05-28', 60.00, 'aberto', NULL, 'MAIO', 2026, '75693146000000060001332501094738562150681001')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CX (CX_SEQ, CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) VALUES (2, '0055-A1', 25, '2026-04-28', 60.00, 'aberto', NULL, 'ABRIL', 2026, '75693146000000060001332501094738562150681002')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CX (CX_SEQ, CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) VALUES (3, '0055-A1', 24, '2026-03-28', 60.00, 'aberto', NULL, 'MARÇO', 2026, '75693146000000060001332501094738562150681003')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CX (CX_SEQ, CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) VALUES (4, '0055-A1', 23, '2026-02-28', 60.00, 'vencido', NULL, 'FEVEREIRO', 2026, '75693146000000060001332501094738562150681004')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CX (CX_SEQ, CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) VALUES (5, '0055-A1', 22, '2026-01-28', 60.00, 'vencido', NULL, 'JANEIRO', 2026, '75693146000000060001332501094738562150681005')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CX (CX_SEQ, CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) VALUES (6, '0055-A1', 21, '2025-12-28', 60.00, 'pago', '2025-12-20', 'DEZEMBRO', 2025, '75693146000000060001332501094738562150681006')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CX (CX_SEQ, CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) VALUES (7, '0055-A1', 20, '2025-11-28', 60.00, 'pago', '2025-11-22', 'NOVEMBRO', 2025, '75693146000000060001332501094738562150681007')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CX (CX_SEQ, CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) VALUES (8, '0055-A1', 19, '2025-10-28', 60.00, 'pago', '2025-10-25', 'OUTUBRO', 2025, '75693146000000060001332501094738562150681008')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CX (CX_SEQ, CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) VALUES (9, '0055-A1', 18, '2025-09-28', 60.00, 'pago', '2025-09-28', 'SETEMBRO', 2025, '75693146000000060001332501094738562150681009')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CX (CX_SEQ, CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) VALUES (10, '0055-A1', 17, '2025-08-28', 60.00, 'pago', '2025-08-20', 'AGOSTO', 2025, '75693146000000060001332501094738562150681010')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CX (CX_SEQ, CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) VALUES (11, '0055-A1', 16, '2025-07-28', 60.00, 'pago', '2025-07-21', 'JULHO', 2025, '75693146000000060001332501094738562150681011')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CX (CX_SEQ, CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) VALUES (12, '0055-A1', 15, '2025-06-28', 60.00, 'pago', '2025-06-25', 'JUNHO', 2025, '75693146000000060001332501094738562150681012')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CX (CX_SEQ, CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) VALUES (13, '0055-A1', 14, '2025-05-28', 55.00, 'pago', '2025-05-20', 'MAIO', 2025, '75693146000000060001332501094738562150681013')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CX (CX_SEQ, CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) VALUES (14, '0055-A1', 13, '2025-04-28', 55.00, 'pago', '2025-04-22', 'ABRIL', 2025, '75693146000000060001332501094738562150681014')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CX (CX_SEQ, CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) VALUES (15, '0055-A1', 12, '2025-03-28', 55.00, 'pago', '2025-03-28', 'MARÇO', 2025, '75693146000000060001332501094738562150681015')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CX (CX_SEQ, CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) VALUES (16, '0055-A1', 11, '2025-02-28', 55.00, 'pago', '2025-02-20', 'FEVEREIRO', 2025, '75693146000000060001332501094738562150681016')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CX (CX_SEQ, CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) VALUES (17, '0055-A1', 10, '2025-01-28', 55.00, 'pago', '2025-01-20', 'JANEIRO', 2025, '75693146000000060001332501094738562150681017')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CX (CX_SEQ, CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) VALUES (18, '0055-A1', 9, '2024-12-28', 55.00, 'pago', '2024-12-15', 'DEZEMBRO', 2024, '75693146000000060001332501094738562150681018')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CX (CX_SEQ, CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) VALUES (19, '0055-A1', 8, '2024-11-28', 55.00, 'pago', '2024-11-20', 'NOVEMBRO', 2024, '75693146000000060001332501094738562150681019')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CX (CX_SEQ, CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) VALUES (20, '0055-A1', 7, '2024-10-28', 55.00, 'pago', '2024-10-22', 'OUTUBRO', 2024, '75693146000000060001332501094738562150681020')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CX (CX_SEQ, CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) VALUES (21, '0055-A1', 6, '2024-09-28', 55.00, 'pago', '2024-09-25', 'SETEMBRO', 2024, '75693146000000060001332501094738562150681021')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CX (CX_SEQ, CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) VALUES (22, '0055-A1', 5, '2024-08-28', 50.00, 'pago', '2024-08-20', 'AGOSTO', 2024, '75693146000000060001332501094738562150681022')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CX (CX_SEQ, CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) VALUES (23, '0055-A1', 4, '2024-07-28', 50.00, 'pago', '2024-07-21', 'JULHO', 2024, '75693146000000060001332501094738562150681023')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CX (CX_SEQ, CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) VALUES (24, '0055-A1', 3, '2024-06-28', 50.00, 'pago', '2024-06-20', 'JUNHO', 2024, '75693146000000060001332501094738562150681024')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CX (CX_SEQ, CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) VALUES (25, '0055-A1', 2, '2024-05-28', 50.00, 'pago', '2024-05-20', 'MAIO', 2024, '75693146000000060001332501094738562150681025')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CX (CX_SEQ, CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) VALUES (26, '0055-A1', 1, '2024-04-28', 50.00, 'pago', '2024-04-20', 'ABRIL', 2024, '75693146000000060001332501094738562150681026')");


            /// Inserir conveniados
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CONVENIADOS (CVN_SEQ, CVN_NOME, CVN_DESCRICAO, CVN_CATEGORIA, CVN_DESCONTO, CVN_ICONE_HASH) VALUES (1, 'Clínica Saúde Total', 'Consultas e Exames', 'saude', 'ATÉ 40% OFF', 'fa-house-medical')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CONVENIADOS (CVN_SEQ, CVN_NOME, CVN_DESCRICAO, CVN_CATEGORIA, CVN_DESCONTO, CVN_ICONE_HASH) VALUES (2, 'Farmácia Popular', 'Medicamentos', 'saude', 'ATÉ 20% OFF', 'fa-pills')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CONVENIADOS (CVN_SEQ, CVN_NOME, CVN_DESCRICAO, CVN_CATEGORIA, CVN_DESCONTO, CVN_ICONE_HASH) VALUES (3, 'Faculdade Futuro', 'Cursos de Graduação', 'educacao', '15% DE DESCONTO', 'fa-graduation-cap')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CONVENIADOS (CVN_SEQ, CVN_NOME, CVN_DESCRICAO, CVN_CATEGORIA, CVN_DESCONTO, CVN_ICONE_HASH) VALUES (4, 'Ótica Visão Clara', 'Lentes e Armações', 'saude', '30% OFF', 'fa-glasses')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CONVENIADOS (CVN_SEQ, CVN_NOME, CVN_DESCRICAO, CVN_CATEGORIA, CVN_DESCONTO, CVN_ICONE_HASH) VALUES (5, 'Academia Corpo & Saúde', 'Mensalidades', 'lazer', '25% OFF', 'fa-dumbbell')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CONVENIADOS (CVN_SEQ, CVN_NOME, CVN_DESCRICAO, CVN_CATEGORIA, CVN_DESCONTO, CVN_ICONE_HASH) VALUES (6, 'Colégio Primeiros Passos', 'Ensino Infantil', 'educacao', '10% OFF', 'fa-school')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CONVENIADOS (CVN_SEQ, CVN_NOME, CVN_DESCRICAO, CVN_CATEGORIA, CVN_DESCONTO, CVN_ICONE_HASH) VALUES (7, 'Restaurante Sabor Caseiro', 'Refeições', 'lazer', '15% OFF', 'fa-utensils')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CONVENIADOS (CVN_SEQ, CVN_NOME, CVN_DESCRICAO, CVN_CATEGORIA, CVN_DESCONTO, CVN_ICONE_HASH) VALUES (8, 'Laboratório Análises', 'Exames Laboratoriais', 'saude', '35% OFF', 'fa-flask')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CONVENIADOS (CVN_SEQ, CVN_NOME, CVN_DESCRICAO, CVN_CATEGORIA, CVN_DESCONTO, CVN_ICONE_HASH) VALUES (9, 'Clínica Odontológica Sorriso', 'Tratamentos Dentários', 'saude', '25% OFF', 'fa-tooth')");
            BancoDados.execSQL("INSERT OR REPLACE INTO TB_CONVENIADOS (CVN_SEQ, CVN_NOME, CVN_DESCRICAO, CVN_CATEGORIA, CVN_DESCONTO, CVN_ICONE_HASH) VALUES (10, 'Livraria do Conhecimento', 'Livros e Materiais', 'educacao', '20% OFF', 'fa-book');");


            // Verificar inserções
            /*
            BancoDados.execSQL("");
            SELECT 'TOTAL LOGIN: ' || COUNT(*) FROM TB_LOGIN;
            SELECT 'TOTAL CLIENTES: ' || COUNT(*) FROM TB_CLI;
            SELECT 'TOTAL DEPENDENTES: ' || COUNT(*) FROM TB_DEPENDENTES;
            SELECT 'TOTAL PARCELAS: ' || COUNT(*) FROM TB_CX;
            SELECT 'TOTAL CONVENIADOS: ' || COUNT(*) FROM TB_CONVENIADOS;
            */
            BancoDados.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao inserir dados de exemplo", e);
        } finally {
            BancoDados.endTransaction();
        }
    }

    public class DadosJson {
        /**
         * Retorna a lista de contratos para a tela selecao_contrato.html
         * @return String no formato JSON Array
         */
        public String getContratosJson() {
            // Traz o código, nome e situação da tabela TB_CLI
            var codigoContrato = getString("CONTRATO_ATIVO");
            String sql = "SELECT CLI_CODIGO, CLI_NOME, CLI_SITUACAO, " +"" +
                    "CASE " +
                    "        WHEN CLI_CODIGO = '" + codigoContrato +"' THEN -1 " +
                    "        ELSE 0 " +
                    "    END AS CLI_SELECIONADO"+
                    " FROM TB_CLI ORDER BY CLI_NOME ASC";
            return obterJsonGenerico(sql);
        }

        // Você pode mover seus outros métodos para cá também:
        public String getCliente(String cliCodigo) {
            String sql = "SELECT * FROM TB_CLI WHERE CLI_CODIGO = '" + cliCodigo + "'";
            return obterJsonGenerico(sql);
        }

        public String getParcelas(String cliCodigo) {
            String sql = "SELECT * FROM TB_CX WHERE CX_CLI_CODIGO = '" + cliCodigo + "' ORDER BY CX_VENCIMENTO DESC";
            return obterJsonGenerico(sql);
        }

        public String getConveniados() {
            String sql = "SELECT * FROM TB_CONVENIADOS ORDER BY CVN_NOME ASC";
            return obterJsonGenerico(sql);
        }
    }

    // ==========================================
    // MÉTODOS DE SINCRONIZAÇÃO (API -> SQLite)
    // ==========================================

    public void apagacliantes() {
        BancoDados.execSQL("DELETE FROM TB_CLI");
    }
    /**
     * Sincroniza os dados principais do cliente
     */
    public void sincronizarClienteApi(int loginSeq, JSONObject cliJson) {
        try {
            BancoDados.beginTransaction();

            // Limpa contratos  antigos
           // BancoDados.execSQL("DELETE FROM TB_CLI");

            // Usamos INSERT OR REPLACE para atualizar caso já exista
            String sql = "INSERT OR REPLACE INTO TB_CLI " +
                    "(CLI_LOGIN_SEQ, CLI_CODIGO, CLI_NOME, CLI_ENDERECO, CLI_ENDERECON, CLI_CIDADE, CLI_UF, CLI_CEP, CLI_TIPOPLANO, CLI_CLI_DATA_TRANS, CLI_SITUACAO) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            BancoDados.execSQL(sql, new Object[]{
                    loginSeq,
                    cliJson.optString("CLI_CODIGO", ""), // Ajuste para a chave retornada na sua API
                    cliJson.optString("CLI_NOME", ""),
                    cliJson.optString("CLI_ENDERECO", ""),
                    cliJson.optString("CLI_ENDERECON", ""),
                    cliJson.optString("CLI_CIDADE", ""),
                    cliJson.optString("CLI_UF", ""),
                    cliJson.optString("CLI_CEP", ""),
                    cliJson.optInt("CLI_TIPOPLANO", 1),
                    cliJson.optString("CLI_CLI_DATA_TRANS", ""),
                    cliJson.optString("CLI_SITUACAO", "ATIVO")
            });

            BancoDados.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao sincronizar dados do cliente", e);
        } finally {
            BancoDados.endTransaction();
            Log.i(TAG, "Sincronizado com sucesso");
        }
    }

    /**
     * Sincroniza os dependentes (Apaga os antigos locais e insere os novos da API)
     */
    public void sincronizarDependentesApi(String cliCodigo, JSONArray dependentesArray) {
        try {
            BancoDados.beginTransaction();

            // Limpa os dependentes antigos do contrato para evitar duplicidade ou dependentes excluídos
            BancoDados.execSQL("DELETE FROM TB_DEPENDENTES WHERE DEP_CLI_CODIGO = ?", new Object[]{cliCodigo});

            String sql = "INSERT INTO TB_DEPENDENTES (DEP_CLI_CODIGO, DEP_NOME, DEP_GRAU_PARENTESCO) VALUES (?, ?, ?)";

            for (int i = 0; i < dependentesArray.length(); i++) {
                JSONObject dep = dependentesArray.getJSONObject(i);

                BancoDados.execSQL(sql, new Object[]{
                        cliCodigo,
                        dep.optString("DEP_NOME", ""),
                        dep.optString("DEP_GRAU_PARENTESCO", "")
                });
            }

            BancoDados.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao sincronizar dependentes", e);
        } finally {
            BancoDados.endTransaction();
        }
    }

    /**
     * Sincroniza o financeiro/parcelas (Apaga as antigas locais e insere as novas da API)
     */
    public void sincronizarParcelasApi(String cliCodigo, JSONArray parcelasArray) {
        try {
            BancoDados.beginTransaction();

            // Limpa parcelas antigas para este contrato
            BancoDados.execSQL("DELETE FROM TB_CX WHERE CX_CLI_CODIGO = ?", new Object[]{cliCodigo});

            String sql = "INSERT INTO TB_CX (CX_CLI_CODIGO, CX_NUMERO, CX_VENCIMENTO, CX_VALOR, CX_STATUS, CX_DT_PGTO, CX_MES, CX_ANO, CX_CODIGO_BARRAS) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            for (int i = 0; i < parcelasArray.length(); i++) {
                JSONObject parc = parcelasArray.getJSONObject(i);

                BancoDados.execSQL(sql, new Object[]{
                        cliCodigo,
                        parc.optInt("CX_NUMERO", 0),
                        parc.optString("CX_VENCIMENTO", ""),
                        parc.optDouble("CX_VALOR", 0.0),
                        parc.optString("CX_STATUS", "aberto"), // pago, aberto, vencido
                        parc.optString("CX_DT_PGTO", null),
                        parc.optString("CX_MES", ""),
                        parc.optInt("CX_ANO", 0),
                        parc.optString("CX_CODIGO_BARRAS", "")
                });
            }

            BancoDados.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao sincronizar parcelas", e);
        } finally {
            BancoDados.endTransaction();
        }
    }
    /**
     * Retorna a quantidade total de registros na tabela TB_CLI
     * @return int - Número de clientes salvos localmente
     */
    public int getQuantidadeClientes() {
        int count = 0;
        try {
            // Conta de forma otimizada direto no banco de dados
            Cursor cursor = BancoDados.rawQuery("SELECT COUNT(*) FROM TB_CLI", null);
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0); // Pega o resultado da primeira coluna (o count)
            }
            cursor.close();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao contar registros em TB_CLI: " + e.getMessage());
        }
        return count;
    }

    // Adicione esta nova função junto das outras de sincronização (ex: perto de sincronizarParcelasApi)
    public void sincronizarConveniadosApi(JSONArray conveniadosArray) {
        try {
            BancoDados.beginTransaction();

            // Limpa os parceiros antigos para não duplicar
            BancoDados.execSQL("DELETE FROM TB_CONVENIADOS");

            String sql = "INSERT INTO TB_CONVENIADOS (CVN_NOME, CVN_DESCRICAO, CVN_CATEGORIA, CVN_DESCONTO, CVN_ICONE_HASH) VALUES (?, ?, ?, ?, ?)";

            for (int i = 0; i < conveniadosArray.length(); i++) {
                JSONObject conv = conveniadosArray.getJSONObject(i);

                BancoDados.execSQL(sql, new Object[]{
                        conv.optString("CONV_NOME", ""),
                        conv.optString("CONV_ENDERECO", ""), // A API manda endereco, o banco salva como descricao
                        conv.optString("CONV_CATEGORIA", "outros"),
                        conv.optString("CONV_DESCONTO", ""),
                        conv.optString("CONV_ICONE", "fa-store") // Padrão se não vier nada
                });
            }

            BancoDados.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao sincronizar conveniados", e);
        } finally {
            BancoDados.endTransaction();
        }
    }
}

