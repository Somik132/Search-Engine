# Поисковой движок 

Проект с подключенными библиотеками лемматизаторами.
Включает в себя функции индексации сайтов и отдельных веб страниц и поиска по этим сайтам.

Содержит веб интерфейс по ссылке http://localhost:8080.
Во вкладке dashboard можно увидеть всю информацию про сайты, указанные в конфигурационном файле application.yaml.



![image](https://github.com/user-attachments/assets/861e57f9-77c1-4d8a-a686-054b595b33ea)

Во вкладке management можно проиндексировать сайт или отдельную страницу. Также можно остановить индексацию досрочно.

![image](https://github.com/user-attachments/assets/11c2c137-14f8-46e9-8fa7-7f2da8be2e8d)

Во вкладке search расположена панель для поиска нужных веб страниц по словам или фразам.

![image](https://github.com/user-attachments/assets/d434b55b-67a3-4969-a641-b749a750379b)

Далее опишу кратко, что происходит в программе: пакет config нужен для хранения данных, указанных в application.yaml.
Контроллеры нужны для связи между локальным сайтом http://localhost:8080 и самой программой. В dto хранятся нужные программе объекты. В model отражения таблиц из базы данных. Репозитории для связи с таблицами из базы данных. В сервисах находится вся логика программы: indexationService и indexationServiceImpl - все что связано с индексацией, LemmaFinder для нахождения лемм слов, Link и ListOfLinks для сногопоточного обхода веб страниц. SearchService и SearchServiceImpl для поиска по сайтам. StatisticsService и StatisticsServiceImpl для вычисления статистики. 

![image](https://github.com/user-attachments/assets/299cd81b-fd63-40c0-a802-4a24e1805443)

В application.yaml можете внести все интересующие вас сайты. Перед поиском обязательно проиндексируйте сайты, нажав на 





# Cтэк используемых технологий
Язык программирования Java. Использовал фреймворк Spring. 
Библиотеки: org.apache.lucene.morphology, org.jsoup, mysql, org.springframework.boot.
Использовал базу данных MySQL.

## Настройки для запуска

### Зависимости

Для успешного скачивания и подключения к проекту зависимостей
из GitHub необходимо настроить Maven конфигурацию в файле `settings.xml`.

В зависимостях, в файле `pom.xml` добавлен репозиторий для получения
jar файлов:

```xml
<repositories>
  <repository>
    <id>skillbox-gitlab</id>
    <url>https://gitlab.skillbox.ru/api/v4/projects/263574/packages/maven</url>
  </repository>
</repositories>
```

Так как для доступа требуется авторизации по токену для получения данных из
публичного репозитория, для указания токена, найдите файл `settings.xml`.

* В Windows он располагается в директории `C:/Users/<Имя вашего пользователя>/.m2`
* В Linux директория `/home/<Имя вашего пользователя>/.m2`
* В macOs по адресу `/Users/<Имя вашего пользователя>/.m2`

>**Внимание!** Актуальный токен, строка которую надо вставить в тег `<value>...</value>`
[находится в документе по ссылке](https://docs.google.com/document/d/1rb0ysFBLQltgLTvmh-ebaZfJSI7VwlFlEYT9V5_aPjc/edit?usp=sharing). 

и добавьте внутри тега `settings` текст конфигурации:

```xml
<servers>
  <server>
    <id>skillbox-gitlab</id>
    <configuration>
      <httpHeaders>
        <property>
          <name>Private-Token</name>
          <value>token</value>
        </property>
      </httpHeaders>
    </configuration>
  </server>
</servers>
```

**Не забудьте поменять токен на актуальный!**

❗️Если файла нет, то создайте `settings.xml` и вставьте в него:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
 https://maven.apache.org/xsd/settings-1.0.0.xsd">

  <servers>
    <server>
      <id>skillbox-gitlab</id>
      <configuration>
        <httpHeaders>
          <property>
            <name>Private-Token</name>
            <value>token</value>
          </property>
        </httpHeaders>
      </configuration>
    </server>
  </servers>

</settings>
```

ℹ️ [Пример готового settings.xml лежит](settings.xml) в корне этого проекта.


**Не забудьте поменять токен на актуальный!**

После этого, в проекте обновите зависимости (Ctrl+Shift+O / ⌘⇧I) или
принудительно обновите данные из pom.xml. 

Для этого вызовите контекстное
у файла `pom.xml` в дереве файла проектов **Project** и выберите пункт меню **Maven -> Reload Project**.


⁉️ Если после этого у вас остается ошибка:

```text
Could not transfer artifact org.apache.lucene.morphology:morph:pom:1.5
from/to gitlab-skillbox (https://gitlab.skillbox.ru/api/v4/projects/263574/packages/maven):
authentication failed for
https://gitlab.skillbox.ru/api/v4/projects/263574/packages/maven/russianmorphology/org/apache/
    lucene/morphology/morph/1.5/morph-1.5.pom,
status: 401 Unauthorized
```

Почистите кэш Maven. Самый надежный способ, удалить директорию:

- Windows `C:\Users\<user_name>\.m2\repository`
- macOs `/Users/<user_name>/.m2/repository`
- Linux `/home/<user_name>/.m2/repository`

где `<user_name>` - имя пользователя под которым вы работаете.

После этого снова попробуйте обновить данный из `pom.xml`

### Настройки подключения к БД

В проект добавлен драйвер для подключения к БД MySQL. Для запуска проекта,
убедитесь, что у вас запущен сервер MySQL 8.x.

🐳 Если у вас установлен докер, можете запустить контейнер с готовыми настройками
под проект командой:

```bash
docker run -d --name=springLemmaExample -e="MYSQL_ROOT_PASSWORD=Kimk7FjT" -e="MYSQL_DATABASE=lemma" -p3306:3306 mysql
```

Имя пользователя по-умолчанию `root`, настройки проекта в `src/resources/application.yml`
соответствуют настройкам контейнера, менять их не требуется.

❗️ Если у вас MacBook c процессором M1, необходимо использовать специальный
образ для ARM процессоров:

```bash
docker run -d --name=springLemmaExample -e="MYSQL_ROOT_PASSWORD=Kimk7FjT" -e="MYSQL_DATABASE=lemma" -p3306:3306 arm64v8/mysql:oracle
```

Если используете MySQL без докера, то создайте бд `index`, `lemma`, `page` и `site` и замените логин и пароль
в файле конфигурации `src/resources/application.yml`:

```yaml
spring:
  datasource:
    username: root # имя пользователя
    password: Kimk7FjT # пароль пользователя
```

После этого, можете запустить проект. Если введены правильные данные,
проект успешно запуститься. Если запуск заканчивается ошибками, изучите текст
ошибок, внесите исправления и попробуйте заново.
