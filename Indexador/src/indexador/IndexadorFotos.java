/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexadorfotos;

import java.io.*;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author alberto carrion leiva
 */
class IndexadorFotos {

    public static void buscarFotos(File dir) {
        try {
            File foto;
            if (dir.isDirectory()) {
                File ficheros[] = dir.listFiles();
                for (File item : ficheros) {
                    buscarFotos(item);
                }
            } else { // ES UNA FOTO Y CALCULAMOS SUS METADATOS
                foto = dir;
                String ruta = foto.getCanonicalPath();
                String[] arrayRuta = ruta.split("\\\\");
                StringBuilder stb = new StringBuilder();

                for(int i = 0; i<arrayRuta.length-1; i++){
                    if(i<arrayRuta.length-2){
                        stb.append(arrayRuta[i]).append("\\");
                    }else{
                        stb.append(arrayRuta[i]);
                    }  
                }
                
                String nombre_foto = foto.getName();
                int index = foto.getName().lastIndexOf(".");
                String extension = foto.getName().substring(index);
                float tamanio = (float) foto.length() / 1024;

                // LLAMAMOS A LA FUNCION PARA CALCULAR SUS METADATOS
                if(extension.equals(".jpg") || extension.equals(".png") || extension.equals(".JPG") || extension.equals(".PNG")){
                    calcularMetadatos(foto, stb.toString(), nombre_foto, extension, tamanio);
                }
            }
        }catch (IOException e) {
            System.err.println("Error en la lectura del fichero");
            System.err.println(e);
        }
    }


    public static void calcularMetadatos(File foto, String ruta, String nombre_foto, String extension, float tamanio) {
        System.out.println("RUTA: "+ruta+", NOMBRE: "+nombre_foto+" , EXTENSION: "+extension+" , TAMAÑO: "+tamanio);
        
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            Connection con = DriverManager.getConnection("jdbc:oracle:thin:inftel16_9/inftel001160@olimpia.lcc.uma.es:1521:edgar");
            
            //LLAMADA A FUNCION INSERTIMAGE
            CallableStatement cstmt = con.prepareCall("{? = call insertimage(?,?,?,?)}");
            cstmt.registerOutParameter(1, Types.NUMERIC);
            cstmt.setString(2, ruta);
            cstmt.setString(3, nombre_foto);
            cstmt.setString(4, extension);
            cstmt.setFloat(5, tamanio);
            cstmt.execute();
            cstmt.close();
            
            // CALCULAMOS LOS METADATOS PASAR EL FILE foto
            // USAMOS LAS LIBRERIAS EXIF
            
            //LLAMADA A PROCEDIMIENTO INSERTMETADATA
            CallableStatement cstmt2 = con.prepareCall("{call insertmetadata(?,?,?,?)}");
            cstmt2.setInt(1, cstmt.getInt(1)); // ID QUE DEVUELVE LA FUNCION INSERTIMAGE (NO TOCAR)
            cstmt2.setString(2, " "); // NOMBRE METADATO DIRECTORIO
            cstmt2.setString(3, " "); // NOMBRE ETIQUETA 
            cstmt2.setString(4, " ");  // VALOR DE LA ETIQUETA PARA ESA FOTO
            cstmt2.execute();
            cstmt2.close();
            
            // CERRAMOS LA CONEXION
            con.close();
            
        } catch (ClassNotFoundException | SQLException ex) {
            System.err.println(ex);
        } 
    }

    public static void main(String argv[]) {
        if (argv.length < 1) {
            System.err.println("ERROR: No se ha especificado el fichero de la ruta");
        } else {
            try {
                File ruta = new File(argv[0]);
                FileReader fr = new FileReader(ruta);
                BufferedReader br = new BufferedReader(fr);
                String linea = br.readLine();
                buscarFotos(new File(linea));
                br.close();
                fr.close();
            } catch (IOException e) {
                System.err.println("Error en la lectura del fichero");
                System.err.println(e);
            }
        }
    }
}
