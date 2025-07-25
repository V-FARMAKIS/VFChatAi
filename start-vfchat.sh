#!/bin/bash

# VFChatAI Startup Script
# This script helps you get VFChatAI up and running quickly

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script configuration
PROJECT_NAME="VFChatAI"
REQUIRED_JAVA_VERSION="17"
REQUIRED_POSTGRES_VERSION="12"

echo -e "${BLUE}🤖 Welcome to ${PROJECT_NAME} Setup!${NC}"
echo "================================================"

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check Java version
check_java() {
    if command_exists java; then
        JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
        if [ "$JAVA_VERSION" -ge "$REQUIRED_JAVA_VERSION" ]; then
            echo -e "${GREEN}✅ Java ${JAVA_VERSION} is installed${NC}"
            return 0
        else
            echo -e "${RED}❌ Java ${REQUIRED_JAVA_VERSION}+ is required, but Java ${JAVA_VERSION} is installed${NC}"
            return 1
        fi
    else
        echo -e "${RED}❌ Java is not installed${NC}"
        return 1
    fi
}

# Function to check PostgreSQL
check_postgres() {
    if command_exists psql; then
        POSTGRES_VERSION=$(psql --version | grep -oE '[0-9]+\.[0-9]+' | head -1 | cut -d. -f1)
        if [ "$POSTGRES_VERSION" -ge "$REQUIRED_POSTGRES_VERSION" ]; then
            echo -e "${GREEN}✅ PostgreSQL ${POSTGRES_VERSION} is installed${NC}"
            return 0
        else
            echo -e "${RED}❌ PostgreSQL ${REQUIRED_POSTGRES_VERSION}+ is required${NC}"
            return 1
        fi
    else
        echo -e "${RED}❌ PostgreSQL is not installed${NC}"
        return 1
    fi
}

# Function to check Maven
check_maven() {
    if command_exists mvn; then
        echo -e "${GREEN}✅ Maven is installed${NC}"
        return 0
    else
        echo -e "${RED}❌ Maven is not installed${NC}"
        return 1
    fi
}

# Function to check environment file
check_env_file() {
    if [ -f ".env" ]; then
        echo -e "${GREEN}✅ .env file exists${NC}"
        
        # Check if required variables are set
        if grep -q "SMTP_USERNAME=your-email@gmail.com" .env || grep -q "GEMINI_API_KEY=your-gemini-api-key-here" .env; then
            echo -e "${YELLOW}⚠️  Please update .env file with your actual credentials${NC}"
            return 1
        else
            echo -e "${GREEN}✅ .env file appears to be configured${NC}"
            return 0
        fi
    else
        echo -e "${YELLOW}⚠️  .env file not found, copying from .env.example${NC}"
        cp .env.example .env
        echo -e "${YELLOW}📝 Please edit .env file with your credentials${NC}"
        return 1
    fi
}

# Function to test database connection
test_db_connection() {
    echo -e "${BLUE}🔍 Testing database connection...${NC}"
    
    # Source environment variables
    if [ -f ".env" ]; then
        set -a
        source .env
        set +a
    fi
    
    DB_HOST=${DB_HOST:-localhost}
    DB_PORT=${DB_PORT:-5432}
    DB_NAME=${DB_NAME:-postgres}
    DB_USER=${DB_USERNAME:-postgres}
    
    if PGPASSWORD="${DB_PASSWORD:-admin}" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1;" >/dev/null 2>&1; then
        echo -e "${GREEN}✅ Database connection successful${NC}"
        return 0
    else
        echo -e "${RED}❌ Cannot connect to database${NC}"
        echo -e "${YELLOW}💡 Make sure PostgreSQL is running and credentials are correct${NC}"
        return 1
    fi
}

# Function to start application
start_application() {
    echo -e "${BLUE}🚀 Starting ${PROJECT_NAME}...${NC}"
    
    # Clean and compile
    echo -e "${BLUE}📦 Installing dependencies...${NC}"
    mvn clean compile -q
    
    # Run the application
    echo -e "${GREEN}🎯 Launching application...${NC}"
    echo -e "${BLUE}📱 Access your app at: http://localhost:8080${NC}"
    echo -e "${BLUE}🌐 Landing page: http://localhost:8080/landingpage.html${NC}"
    echo -e "${YELLOW}Press Ctrl+C to stop the application${NC}"
    echo ""
    
    mvn spring-boot:run
}

# Function to show help
show_help() {
    echo -e "${BLUE}Usage: $0 [OPTION]${NC}"
    echo ""
    echo "Options:"
    echo "  start     Start the application (default)"
    echo "  check     Check system requirements only"
    echo "  setup     Setup environment and dependencies"
    echo "  test      Run tests"
    echo "  clean     Clean build artifacts"
    echo "  help      Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                # Start the application"
    echo "  $0 check          # Check requirements"
    echo "  $0 setup          # Setup environment"
}

# Function to run setup
run_setup() {
    echo -e "${BLUE}🔧 Setting up ${PROJECT_NAME}...${NC}"
    
    # Copy environment file if needed
    if [ ! -f ".env" ]; then
        cp .env.example .env
        echo -e "${GREEN}✅ Created .env file from template${NC}"
    fi
    
    # Install dependencies
    echo -e "${BLUE}📦 Installing Maven dependencies...${NC}"
    mvn clean install -DskipTests -q
    
    echo -e "${GREEN}✅ Setup completed!${NC}"
    echo -e "${YELLOW}📝 Don't forget to update your .env file with actual credentials${NC}"
}

# Function to run tests
run_tests() {
    echo -e "${BLUE}🧪 Running tests...${NC}"
    mvn test
}

# Function to clean
run_clean() {
    echo -e "${BLUE}🧹 Cleaning build artifacts...${NC}"
    mvn clean
    echo -e "${GREEN}✅ Clean completed${NC}"
}

# Main execution
main() {
    local action=${1:-start}
    
    case $action in
        "start")
            echo -e "${BLUE}🔍 Checking system requirements...${NC}"
            
            # Check requirements
            JAVA_OK=0
            MAVEN_OK=0
            POSTGRES_OK=0
            ENV_OK=0
            DB_OK=0
            
            check_java || JAVA_OK=1
            check_maven || MAVEN_OK=1
            check_postgres || POSTGRES_OK=1
            check_env_file || ENV_OK=1
            
            if [ $JAVA_OK -ne 0 ] || [ $MAVEN_OK -ne 0 ] || [ $POSTGRES_OK -ne 0 ]; then
                echo -e "${RED}❌ System requirements not met${NC}"
                echo -e "${YELLOW}💡 Please install missing requirements and try again${NC}"
                exit 1
            fi
            
            if [ $ENV_OK -ne 0 ]; then
                echo -e "${YELLOW}⚠️  Please configure your .env file and run again${NC}"
                exit 1
            fi
            
            test_db_connection || DB_OK=1
            
            if [ $DB_OK -ne 0 ]; then
                echo -e "${YELLOW}💡 Starting without database connection test...${NC}"
            fi
            
            echo ""
            start_application
            ;;
        "check")
            echo -e "${BLUE}🔍 Checking system requirements...${NC}"
            check_java
            check_maven
            check_postgres
            check_env_file
            test_db_connection
            echo -e "${GREEN}✅ Requirements check completed${NC}"
            ;;
        "setup")
            run_setup
            ;;
        "test")
            run_tests
            ;;
        "clean")
            run_clean
            ;;
        "help"|"--help"|"-h")
            show_help
            ;;
        *)
            echo -e "${RED}❌ Unknown option: $action${NC}"
            show_help
            exit 1
            ;;
    esac
}

# Check if script is being sourced or executed
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
