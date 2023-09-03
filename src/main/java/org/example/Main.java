package org.example;

import java.io.*;
import org.json.JSONObject;
import org.json.JSONArray;

import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcWriter;
import org.marc4j.marc.*;
import org.apache.commons.io.IOUtils;
import org.marc4j.marc.Record;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 *
 * @author Alptwo
 */

public class Main {
    public static void main(String[] args) throws Exception {
        jsonToMarc("D:\\Development\\JavaProjects\\JsonToMarc21Converter\\src\\main\\java\\org\\example\\alptug_ornek_100.json");
    }
    public static void jsonToMarc(String jsonFileLoc) throws Exception {
        File f = new File(jsonFileLoc);
        if (f.exists()) {
            InputStream is = new FileInputStream(jsonFileLoc);
            String jsonTxt = IOUtils.toString(is, "UTF-8");
            JSONObject json = new JSONObject(jsonTxt);
            //hits değeri var json içerisinde onun içinde bütün jsonlar ona göre düzenleyeceksin()json->map->hits->value->map->hits->value->myArrayList->0,1,2,3,4,5,6,...
            //Buraya bir for yazıyoruz kaç tane JSON varsa bir dosya içerisinde onların hepsini MARC21 şeklinde tek dosya içerisinde yazıcaz
            MarcFactory factory = MarcFactory.newInstance();
            Record record = factory.newRecord("00000nam a2200000Ka 4500");

            Leader leader = record.getLeader();
            leader.setRecordStatus('n');
            leader.setTypeOfRecord('a');

            try {
                String jsonId = json.getString("_id");
                // 001 Alanını belirle
                ControlField controlField001 = factory.newControlField("001", jsonId);
                record.addVariableField(controlField001);
            }catch(Exception e) {
                System.out.println("Hata oluştu: " + e.getMessage());
            }

            try {
                try {
                    // 020 Alanı belirle (ISBN)
                    JSONArray kunyeISBNISSNArray = json.getJSONObject("_source").getJSONArray("kunyeISBNISSN");
                    DataField dataField020 = factory.newDataField("020", '#', '#');
                    dataField020.addSubfield(factory.newSubfield('a', kunyeISBNISSNArray.getString(0)));
                    record.addVariableField(dataField020);
                }catch(Exception e) {
                    System.out.println("Hata oluştu: " + e.getMessage());
                }
                try {
                    // 022 Alanı belirle (ISSN) (ISSN aynı zamanda 830'a da eklenecek https://www.loc.gov/marc/bibliographic/bd830.html)
                    JSONArray kunyeISBNISSNArray = json.getJSONObject("_source").getJSONArray("kunyeISBNISSN");
                    DataField dataField022 = factory.newDataField("022", '#', '#');
                    dataField022.addSubfield(factory.newSubfield('a', kunyeISBNISSNArray.getString(1)));
                    record.addVariableField(dataField022);
                }catch(Exception e) {
                    try {
                        String information = json.getJSONObject("_source").getString("information");
                        DataField dataField022 = factory.newDataField("022", '#', '#');

                        //ilk <br> veya ilk " " arasındaki verileri alan regex
                        String issnPattern = "ISSN:\\s*(.*?)(<br>|\\s)";
                        Pattern pattern = Pattern.compile(issnPattern);
                        Matcher matcher = pattern.matcher(information);

                        if (matcher.find()) {
                            String issnValue = matcher.group(1).trim(); // İlk grup ISSN değerini içerir
                            dataField022.addSubfield(factory.newSubfield('a', issnValue));
                            record.addVariableField(dataField022);
                        } else {
                            System.out.println("ISSN bulunamadı.");
                        }
                    }catch(Exception ex) {
                        System.out.println("Hata oluştu: " + e.getMessage());
                    }
                }
            }catch(Exception e) {
                System.out.println("Hata oluştu: " + e.getMessage());
            }

            try {
                String doiNumber = json.getJSONObject("_source").getString("doi");
                // 024 Alanı belirle
                DataField dataField024 = factory.newDataField("024", '7', ' ');
                dataField024.addSubfield(factory.newSubfield('a', doiNumber));
                dataField024.addSubfield(factory.newSubfield('2', "doi"));
                record.addVariableField(dataField024);
            }catch(Exception e) {
                System.out.println("Hata oluştu: " + e.getMessage());
            }

            try {
                String language = json.getJSONObject("_source").getString("languages");
                // 041 Alanı belirle(dil(languages))
                if(!language.equals("0")) {
                    DataField dataField041 = factory.newDataField("041", ' ', ' ');
                    dataField041.addSubfield(factory.newSubfield('a', language));
                    record.addVariableField(dataField041);
                }
            }catch(Exception e) {
                System.out.println("Hata oluştu: " + e.getMessage());
            }

            try {
                JSONArray authorsArray = json.getJSONObject("_source").getJSONArray("authors");
                // 100 Alanı belirle (authors)
                if (!authorsArray.getString(0).equals("0")) {
                    DataField dataField100 = factory.newDataField("100", '0', ' ');
                    for(int i = 0; i < authorsArray.length(); i++) {
                        String author = authorsArray.getString(i);
                        dataField100.addSubfield(factory.newSubfield('a', author.replaceFirst(" ", ", ")));
                    }
                    dataField100.addSubfield(factory.newSubfield('e', "author"));
                    record.addVariableField(dataField100);
                }
            }catch(Exception e) {
                System.out.println("Hata oluştu: " + e.getMessage());
            }

            try {
                String bookTitle = json.getJSONObject("_source").getString("title");
                // 245 Alanı belirle
                DataField dataField245 = factory.newDataField("245", '0', '0');
                dataField245.addSubfield(factory.newSubfield('a', bookTitle));
                record.addVariableField(dataField245);
            }catch(Exception e) {
                System.out.println("Hata oluştu: " + e.getMessage());
            }

            try {
                String publishDate = json.getJSONObject("_source").getString("year");
                String publisher = json.getJSONObject("_source").getString("publisher");
                // 264 Alanı belirle
                if(!publisher.equals("0") && !publishDate.equals("0")) {
                    if (publisher.endsWith(",")) {
                        publisher = publisher.substring(0, publisher.length() - 1);
                    }
                    DataField dataField264 = factory.newDataField("264", ' ', '1');
                    dataField264.addSubfield(factory.newSubfield('b', publisher));
                    dataField264.addSubfield(factory.newSubfield('c', publishDate));
                    record.addVariableField(dataField264);
                }else if(!publisher.equals("0")) {
                    if (publisher.endsWith(",")) {
                        publisher = publisher.substring(0, publisher.length() - 1);
                    }
                    DataField dataField264 = factory.newDataField("264", ' ', '1');
                    dataField264.addSubfield(factory.newSubfield('b', publisher));
                    record.addVariableField(dataField264);
                }else if(!publishDate.equals("0")) {
                    DataField dataField264 = factory.newDataField("264", ' ', '1');
                    dataField264.addSubfield(factory.newSubfield('c', publishDate));
                    record.addVariableField(dataField264);
                }
            }catch(Exception e) {
                System.out.println("Hata oluştu: " + e.getMessage());
            }

            try {
                try {
                    String information = json.getJSONObject("_source").getString("information");
                    int index = information.indexOf("Sayfa:") + "Sayfa:".length();

                    // 300 Alanı belirle
                    DataField dataField300 = factory.newDataField("300", '#', '#');
                    dataField300.addSubfield(factory.newSubfield('a', (information.substring(index+1))));
                    dataField300.addSubfield(factory.newSubfield('f', ("page(s)")));
                    try{
                        String patternString = "Cilt:\\s*(.*?)<br>";
                        Pattern pattern = Pattern.compile(patternString);
                        Matcher matcher = pattern.matcher(information);
                        if (matcher.find()) {
                            String volume = matcher.group(1).trim();
                            dataField300.addSubfield(factory.newSubfield('a', volume));
                            dataField300.addSubfield(factory.newSubfield('f', "volume"));
                        } else {
                            System.out.println("Veri bulunamadı.");
                        }
                        patternString = "Sayı:\\s*(.*?)<br>";
                        pattern = Pattern.compile(patternString);
                        matcher = pattern.matcher(information);
                        if (matcher.find()) {
                            String issue = matcher.group(1).trim();
                            dataField300.addSubfield(factory.newSubfield('a', issue));
                            dataField300.addSubfield(factory.newSubfield('f', "issue"));
                        } else {
                            System.out.println("Veri bulunamadı.");
                        }
                    }catch(Exception e) {
                        System.out.println("Hata oluştu: " + e.getMessage());
                    }
                    record.addVariableField(dataField300);
                }catch(Exception e) {
                    String page = json.getJSONObject("_source").getString("last_page");
                    String issue = json.getJSONObject("_source").getString("issue");
                    String volume = json.getJSONObject("_source").getString("volume");
                    // 300 Alanı belirle
                    DataField dataField300 = factory.newDataField("300", '#', '#');
                    if(!page.equals("0")) {
                        dataField300.addSubfield(factory.newSubfield('a', page));
                        dataField300.addSubfield(factory.newSubfield('f', ("page(s)")));
                    }
                    if(!volume.equals("0")) {
                        dataField300.addSubfield(factory.newSubfield('a', volume));
                        dataField300.addSubfield(factory.newSubfield('f', "volume"));
                    }
                    if(!issue.equals("0")) {
                        dataField300.addSubfield(factory.newSubfield('a', issue));
                        dataField300.addSubfield(factory.newSubfield('f', "issue"));
                    }
                    record.addVariableField(dataField300);
                }
            }catch(Exception e){
                System.out.println("Hata oluştu: " + e.getMessage());
            }

            try {
                String summary = json.getJSONObject("_source").getString("summary");

                if(!summary.equals("0")) {
                    if (summary.startsWith("0")) {
                        summary = summary.substring(1, summary.length());
                    }
                    // 520 Alanı belirle(##)
                    DataField dataField520 = factory.newDataField("520", '#', '#');
                    dataField520.addSubfield(factory.newSubfield('b', summary));
                    record.addVariableField(dataField520);
                }
            }catch(Exception e) {
                System.out.println("Hata oluştu: " + e.getMessage());
            }

            //burda şöyle bir hata ortaya çıkabiliyor bazı verilede classification ile classification2 içindeki veriler aynı oraya bir if koymak lazım aynı ise 2 kere yazdırmasın
            try {
                JSONArray classificationArray = json.getJSONObject("_source").getJSONArray("classification");
                // 650 Alanı belirle
                if(classificationArray != null) {
                    for(int i = 0; i < classificationArray.length(); i++) {
                        DataField dataField650 = factory.newDataField("650", '1', '4');
                        JSONObject classificationObj = classificationArray.getJSONObject(i);
                        String discipline = classificationObj.getString("discipline");
                        dataField650.addSubfield(factory.newSubfield('a', discipline));
                        record.addVariableField(dataField650);
                    }
                    for(int i = 0; i < classificationArray.length(); i++) {
                        JSONObject classificationObj = classificationArray.getJSONObject(i);
                        JSONArray subDisciplineArray = classificationObj.getJSONArray("sub_discipline");
                        for(int j = 0; j < subDisciplineArray.length(); j++) {
                            DataField dataField650sub = factory.newDataField("650", '2', '4');
                            JSONObject subDisciplineObj = subDisciplineArray.getJSONObject(j);
                            String discipline = subDisciplineObj.getString("discipline");
                            dataField650sub.addSubfield(factory.newSubfield('a', discipline));
                            record.addVariableField(dataField650sub);
                        }
                    }
                    try {
                        JSONArray classificationArray2 = json.getJSONObject("_source").getJSONArray("classification2");
                        for(int i = 0; i < classificationArray2.length(); i++) {
                            DataField dataField650 = factory.newDataField("650", '1', '4');
                            JSONObject classificationObj = classificationArray2.getJSONObject(i);
                            String discipline = classificationObj.getString("discipline");
                            dataField650.addSubfield(factory.newSubfield('a', discipline));
                            record.addVariableField(dataField650);
                        }
                        for(int i = 0; i < classificationArray2.length(); i++) {
                            JSONObject classificationObj = classificationArray2.getJSONObject(i);
                            JSONArray subDisciplineArray = classificationObj.getJSONArray("sub_discipline");
                            for(int j = 0; j < subDisciplineArray.length(); j++) {
                                DataField dataField650sub = factory.newDataField("650", '2', '4');
                                JSONObject subDisciplineObj = subDisciplineArray.getJSONObject(j);
                                String discipline = subDisciplineObj.getString("discipline");
                                dataField650sub.addSubfield(factory.newSubfield('a', discipline));
                                record.addVariableField(dataField650sub);
                            }
                        }
                    }catch (Exception e) {
                        System.out.println("Hata oluştu: " + e.getMessage());
                    }
                }else {
                    JSONArray classificationArray2 = json.getJSONObject("_source").getJSONArray("classification2");
                    for(int i = 0; i < classificationArray2.length(); i++) {
                        JSONObject classificationObj = classificationArray2.getJSONObject(i);
                        JSONArray subDisciplineArray = classificationObj.getJSONArray("sub_discipline");
                        for(int j = 0; j < subDisciplineArray.length(); j++) {
                            DataField dataField650sub = factory.newDataField("650", '2', '4');
                            JSONObject subDisciplineObj = subDisciplineArray.getJSONObject(j);
                            String discipline = subDisciplineObj.getString("discipline");
                            dataField650sub.addSubfield(factory.newSubfield('a', discipline));
                            record.addVariableField(dataField650sub);
                        }
                    }
                }
            }catch(Exception e) {
                System.out.println("Hata oluştu: " + e.getMessage());
                try{
                    String topics = json.getJSONObject("_source").getString("topics");
                    if(!topics.equals("0")) {
                        DataField dataField650 = dataField650 = factory.newDataField("650", '1', '4');
                        dataField650.addSubfield(factory.newSubfield('a', topics));
                        record.addVariableField(dataField650);
                    }
                }catch(Exception ex) {
                    System.out.println("Hata oluştu: " + ex.getMessage());
                }
            }

            try {
                String publisher = json.getJSONObject("_source").getString("publisher");
                if(!publisher.equals("0")) {
                    if (publisher.endsWith(",")) {
                        publisher = publisher.substring(0, publisher.length() - 1);
                    }
                    // 710 Alanı belirle
                    DataField dataField710 = factory.newDataField("710", ' ', ' ');
                    dataField710.addSubfield(factory.newSubfield('a', publisher));
                    record.addVariableField(dataField710);
                }
            }catch(Exception e) {
                System.out.println("Hata oluştu: " + e.getMessage());
            }

            try {
                String pdfUrl = json.getJSONObject("_source").getString("pdfurl");
                // 856 Alanı belirle (4(HTTP), 0(Resource))
                if(!pdfUrl.equals("0")) {
                    DataField dataField700_1 = factory.newDataField("856", '4', '0');
                    dataField700_1.addSubfield(factory.newSubfield('u', pdfUrl));
                    record.addVariableField(dataField700_1);
                }
                String sourceUrl = json.getJSONObject("_source").getString("sourceurl");
                if(!sourceUrl.equals("0") && !sourceUrl.equals(pdfUrl)) {
                    DataField dataField700_2 = factory.newDataField("856", '4', '0');
                    dataField700_2.addSubfield(factory.newSubfield('u', sourceUrl));
                    record.addVariableField(dataField700_2);
                }
            }catch(Exception e) {
                System.out.println("Hata oluştu: " + e.getMessage());
            }

            try {
                OutputStream outputStream = new FileOutputStream("my_marc_record.mrc");
                MarcWriter writer = new MarcStreamWriter(outputStream, "UTF-8");
                writer.write(record);
                writer.close();
                System.out.println("MARC kaydı başarıyla dosyaya kaydedildi.");
            } catch (IOException e) {
                System.err.println("Dosya kaydetme hatası: " + e.getMessage());
            }
        }
    }
}