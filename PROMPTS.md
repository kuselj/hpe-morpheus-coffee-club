- [AI Usage Summary](#ai-usage-summary)
  - [1. Architecture \& Design](#1-architecture--design)
    - [1.1 Framework Selection](#11-framework-selection)
    - [1.2 Licensing \& Compliance](#12-licensing--compliance)
  - [2. Cloud Deployment Options Exploration](#2-cloud-deployment-options-exploration)
  - [3. Claude Prompt Design](#3-claude-prompt-design)
    - [3.1 Determine format to use for Claude prompt to build the web app (Since 1st time using Claude for this).](#31-determine-format-to-use-for-claude-prompt-to-build-the-web-app-since-1st-time-using-claude-for-this)
    - [3.2 Figure out Claude prompt specifics to build the web app as expected (Since 1st time using Claude for this).](#32-figure-out-claude-prompt-specifics-to-build-the-web-app-as-expected-since-1st-time-using-claude-for-this)
  - [4. Initial App Generation via Claude](#4-initial-app-generation-via-claude)
  - [5. Claude Code Prompts During Development](#5-claude-code-prompts-during-development)

# AI Usage Summary
This file documents the AI prompts used during the development of this project and explains their contribution to the final solution.

## 1. Architecture & Design
### 1.1 Framework Selection
* **Prompt (Gemini):** 
  > "What are the best frameworks to use via claude when you want to build a web app in which the pages display correctly on the phone and desktop"
* **Contribution:** Indicated Vite + React with either Tailwind CSS or Bootstrap for responsive layouts. So went with React and then Tailwind CSS since seems most flexible.

### 1.2 Licensing & Compliance
* **Prompt (Gemini):** 
  > "Is react and tailwind free to use commercially"
* **Contribution:** Indicated 'Yes' and under MIT license so can go ahead since no need to pay royalties, buy licenses, or ask for permission.

## 2. Cloud Deployment Options Exploration
* **Prompt:**
  > "Is there a free cloud storage where you can push your Spring Boot application to"
* **Contribution:** Suggested Render as the 1st choice since it can connect to GitHub and auto deploy updated app. Only small downside: free instances "spin down" (go to sleep) after 15 minutes of inactivity. Next visitor will experience 50–60 second delay while the server wakes up. So choose it since Railway might have credit limit and fly.io config/setup can sometimes be more tricky.
  
## 3. Claude Prompt Design
### 3.1 Determine format to use for Claude prompt to build the web app (Since 1st time using Claude for this).
* **Prompt (Gemini):** 
  > "So when you perform create project in claude code app can you put all the requirements and expectations in the What are you trying to achieve box? What is the general format for this?"
* **Contribution:** Indicated 'Yes' and asked for the basic high level requirements and provided the basic format example for this which I then used as base and added more context and customization as needed.

### 3.2 Figure out Claude prompt specifics to build the web app as expected (Since 1st time using Claude for this).
* **Note: Just mentioned '[Various]' as prompt as to not clog up this file with 70+ clarifying question prompts** 
* **Prompt (Gemini):** 
  > "[Various 50+ clarifying prompts]"
* **Contribution:** It helped me understand how to specify some of the various parts e.g. tech stack, database structure, naming conventions and mappings as well as point out some improvements of my prompt based on the requirements.

## 4. Initial App Generation via Claude
* **Prompt (Claude)**
```text
Please build a full-stack fully responsive single-page web application (Mobile-First layout but equally suited for desktop), called "HPE Morpheus Coffee Club". 

Core Tech Stack:
----------------
Utilize latest versions of the following:
- Backend: Spring Boot for controllers & services with strict input validation.
- Database: H2 database using local file persistence (`jdbc:h2:file:./data/coffeedb`) to ensure seamless data persistence across server restarts.
- Frontend: React (Vite with TypeScript) for web page components and layout while using Tailwind CSS for styling and ensuring that common & repeating style elements are contained in a separate CSS file to ensure optimal maintainability.
- Tests: JUnit tests for all the various backend scenarios.
- Build Tool: Maven or Gradle

Functional Requirements:
------------------------
There are 7 coworkers (including Bob who drinks cappuccino, Jim who drinks black coffee, and 5 others with custom drinks). They take turns to pay for everyone's coffee i.e. only one person pays per day. Not all drinks cost the same so the app should also ensure fairness by selecting the coworker to pay for the group based on the net difference between the total amount they have paid and the total cost of the drinks they have personally consumed over their lifetime.

Design Requirements:
--------------------
1. Group order page: a screen with the following:
- A table listing with columns for coworker order details pre-populated from previous order. If no previous order: only the header row should be displayed. The columns are:
  -- Name: the coworker's name. Default is empty but a required field.
  -- Drink: previous order's drink associated with the name (Empty if no previous drink found). A required field.
  -- Price: format the number with two decimal places 0.00. Value should be previous order's drink price that is greater than zero, associated with the most recent order for that person (Default to 0 if no previous value could be found). A required field.
  -- Remove: a checkbox that is unchecked by default. When the user clicks on it and it is checked, the Price should be set to 0. If the user unchecks it again it restores the price to the previous price value. Once submitted with Remove checked it will also permanently delete them from future pre-populations.
- The initial table values should be sorted alphabetically based on the Name column. 
- Above the table there should be this note: "Note: If person is not ordering today, enter 0 (zero) in the Price field."      
- Below the table on the left side there should be a button named 'Add Person'. When clicked, a new row should be added to the table with the column values set and populated according to the default column values.
- Below the table on the right side there are the following fields:
-- Payer: Not editable with initial value of 'TBD (Until all fields filled)'. Once all the table fields are filled and pass basic input validation (Default values to be considered as filled) it determines and displays the name of coworker selected to pay. To determine which coworker has to pay and keep it fair: the first coworker in the list with the lowest net difference (total amount they have paid - the total cost of the drinks they have personally consumed over their lifetime), that participates today (Today's price not 0 and not indicated to be removed), is selected and their name displayed in the field. They are the one who pays for the group and their total paid for today is the total cost of the group order.   
-- Total: the total cost of the group order. Initially populate with the total price for all drinks in the table that don't have the Remove checkbox checked. If no table entries the value should be 0. The total should dynamically update as prices are changed in the table of which the Remove button is not checked. Format the number with two decimal places 0.00
- At the bottom right of the screen is a button named 'Submit' that validates the input and if passed, it saves the values to the database. If the table has no rows it should give the error message: "At least 1 person is required. Click the 'Add Person' button to add an individual."  

Database & Code Naming Requirements:
------------------------------------
Map the coworker data strictly using these conventions:
- Database Table Name (snake_case): hpe_morpheus_coffee_club
- Database Columns (snake_case): order_date, name, drink, price, total_paid_today, is_removed
- Java Entity Variables (camelCase): orderDate, name, drink, price, totalPaidToday, isRemoved


Data Initialization:
--------------------
Pre-populate the blank database on initial startup with two default records:
1. date: yesterday's date | name: Bob | drink: Cappuccino | price: 0.00 | total_paid_today: 0.00 | is_removed: N
2. date: yesterday's date | name: Jim | drink: Black Coffee | price: 0.00 | total_paid_today: 0.00 | is_removed: N


Theme & Styling Requirements (Atmospheric Coffeehouse Aesthetic):
-----------------------------------------------------------------
- Aesthetic Style: Rich, moody, and atmospheric coffee shop environment inspired by traditional Ethiopian coffee ceremonies.
- Color Palette: 
  -- Background Canvas: Very dark charcoal/espresso base (e.g., Tailwind 'stone-950' or 'neutral-900').
  -- Accent Tones: Warm firelight amber, deep coffee browns, and glowing gold indicators.
- Graphic & Background Art Treatment:
  -- Background: Overlay a full-bleed, high-quality dark photographic asset or texture placeholder representing a rustic coffee setting (burlap sacks, roasting beans, or a traditional clay Jebena pot over a charcoal fire with soft rising smoke).
  -- Contrast Filter: Wrap the background in a dark semi-transparent Tailwind layer (`bg-stone-950/70 backdrop-blur-sm`) to keep the interface deeply atmospheric yet dark enough for text to pop.
  -- UI Workspace Panels: Use elegant, semi-transparent frosted glass elements (`bg-stone-900/80 border border-stone-800 backdrop-blur-md text-stone-100`) for the tables and input fields.
  -- Visual elements in the web UI must use human-readable, regular casing with proper spaces (e.g., "Name", "Total Paid Today", "Order Date").

Build & Deploy Instructions:
----------------------------
- Automatically generate a comprehensive README.md file documenting clear instructions on how to build and run the solution. Configure the application build scripts to support three distinct, clean operational workflows:
1. 'dev' Profile (Active Local Coding with Hot-Reload):
   - Keep Frontend and Backend directories decoupled during active local coding.
   - Configure the React Vite server to run independently on its own port and include a proxy configuration block directing all '/api/*' fetch requests smoothly to the Spring Boot backend running on port 8080.
   - This setup must support full Hot Module Replacement (HMR) for instant UI updates.

2. 'prod' Profile (Local Executable Standalone JAR):
   - Configure a dedicated Maven/Gradle build profile named 'prod'.
   - When running with the 'prod' profile, automate a build pipeline that triggers 'npm run build' inside the React layout folder and copies the compiled static assets directly into Spring Boot’s compiled static resources target directory.
   - The final artifact must output a single standalone executable JAR that serves both the API endpoints and the static frontend UI on port 8080.

3. 'prod-cloud' Configuration (Automated Render Cloud Deployment via Dockerfile):
   - Create a root-level 'Dockerfile' to handle containerization on Render.
   - The Dockerfile must use a multi-stage environment: Stage 1 builds the React production frontend using Node, Stage 2 compiles the Spring Boot source using a JDK image while injecting Stage 1 static outputs, and Stage 3 provisions a minimal JRE runtime to execute the final standalone JAR.
   - Ensure the application dynamically reads the 'PORT' environment variable provided by Render (falling back to 8080 if not set) so that the web service boots successfully.

- Add the following assumptions above the build and run instructions in an 'Assumptions' section:
To keep the development within reasonable time limits the following assumptions were made:
1. Prioritized polished front-end UI over front-end testing and rely on the back-end validation instead. However in regular production intended deployments I would go with Playwright for the end to end testing.
2. Tax calculations and additions were not incorporated.
3. Historical order changes/corrections not catered for.

Claude Specific Prompt File Instructions:
-----------------------------------------
Generate a root-level `PROMPTS_Claude.md` file and append to it throughout our session any subsequent user prompts I give you (such as follow-up feature requests, bug fixes, or refinements etc.). Keep the file concise: only log the prompts I provide, followed by a brief 1-sentence note on the result/solution/contribution for each in the following format:
* **Prompt:**
  > "<Prompt>"
* **Contribution:** <result/solution/contribution>

```
* **Contribution:** Claude built out the initial app based on these requirements which I then verified and tweaked according to what I wanted it to look like and function.

## 5. Claude Code Prompts During Development
You can refer to the 'PROMPTS_Claude.md' file showcasing the subsequent user prompts I used (such as follow-up feature requests, bug fixes, or refinements etc.). 
