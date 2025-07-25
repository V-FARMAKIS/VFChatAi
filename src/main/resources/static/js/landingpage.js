
let isGoogleInitialized = false;
const GOOGLE_CLIENT_ID = "700535710372-fndkfdnd12uk4uoe2m1ie1dsnt5c2fqm.apps.googleusercontent.com";
let countdownInterval;
let liveFeedInterval;
let testimonialInterval;


document.addEventListener('DOMContentLoaded', function() {
    try {
        initializeGoogleAuth();
        initializeAnimations();
        setupEventListeners();
        initializeCountdown();
        initializeLiveFeed();
        initializeTestimonials();
        initializeNeuralMesh();
        initializeParticles();
    } catch (error) {
        console.error('Initialization error:', error);
    }
});


function initializeGoogleAuth() {

    console.log('Google OAuth disabled for testing');
    isGoogleInitialized = false;
}


function initializeAnimations() {

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('visible');
            }
        });
    }, { threshold: 0.1 });

    document.querySelectorAll('.feature-card, .stat').forEach(el => {
        observer.observe(el);
    });
}


function setupEventListeners() {

    window.addEventListener('click', function(event) {
        const authModal = document.getElementById('authModal');
        if (event.target === authModal) {
            closeAuthModal();
        }
    });
    

    document.addEventListener('keydown', function(event) {
        if (event.key === 'Escape') {
            closeAuthModal();
        }
    });
}


function initializeCountdown() {
    updateCountdown();
    countdownInterval = setInterval(updateCountdown, 1000);
}

function updateCountdown() {
    const now = new Date();
    const tomorrow = new Date(now);
    tomorrow.setDate(tomorrow.getDate() + 1);
    tomorrow.setHours(0, 0, 0, 0);
    
    const diff = tomorrow.getTime() - now.getTime();
    
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((diff % (1000 * 60)) / 1000);
    
    const hoursEl = document.getElementById('hours');
    const minutesEl = document.getElementById('minutes');
    const secondsEl = document.getElementById('seconds');
    const finalCountdownEl = document.getElementById('finalCountdown');
    
    if (hoursEl) hoursEl.textContent = hours.toString().padStart(2, '0');
    if (minutesEl) minutesEl.textContent = minutes.toString().padStart(2, '0');
    if (secondsEl) secondsEl.textContent = seconds.toString().padStart(2, '0');
    if (finalCountdownEl) finalCountdownEl.textContent = `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
}


function initializeLiveFeed() {
    const liveFeed = document.getElementById('liveFeed');
    if (!liveFeed) {
        return;
    }
    
    const activities = [
        { name: 'Sarah M.', action: 'created a stunning AI artwork', time: 'Just now' },
        { name: 'Alex K.', action: 'generated 5 marketing images', time: '2 min ago' },
        { name: 'Emily R.', action: 'wrote an AI poem', time: '3 min ago' },
        { name: 'Mike J.', action: 'created a business logo', time: '4 min ago' },
        { name: 'Lisa C.', action: 'generated blog content', time: '5 min ago' }
    ];
    
    let currentIndex = 0;
    
    function updateLiveFeed() {
        try {
            const activity = activities[currentIndex];
            liveFeed.innerHTML = `
                <div class="live-activity">
                    <div class="activity-avatar">👤</div>
                    <div class="activity-content">
                        <div class="activity-text">
                            <strong>${activity.name}</strong> ${activity.action}
                        </div>
                        <div class="activity-time">${activity.time}</div>
                    </div>
                    <div class="activity-status">🔴 LIVE</div>
                </div>
            `;
            
            currentIndex = (currentIndex + 1) % activities.length;
        } catch (error) {
            console.error('Error updating live feed:', error);
        }
    }
    
    updateLiveFeed();
    liveFeedInterval = setInterval(updateLiveFeed, 3000);
}


function initializeTestimonials() {
    const testimonials = document.querySelectorAll('.testimonial');
    if (testimonials.length === 0) {
        return;
    }
    
    let currentTestimonial = 0;
    
    function showNextTestimonial() {
        testimonials[currentTestimonial].classList.remove('active');
        currentTestimonial = (currentTestimonial + 1) % testimonials.length;
        testimonials[currentTestimonial].classList.add('active');
    }
    
    testimonialInterval = setInterval(showNextTestimonial, 5000);
}


function showNotification(message, type = 'info') {

    const existing = document.querySelector('.notification');
    if (existing) {
        existing.remove();
    }

    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.textContent = message;

    document.body.appendChild(notification);


    setTimeout(() => {
        notification.classList.add('show');
    }, 10);
    

    setTimeout(() => {
        notification.classList.remove('show');
        setTimeout(() => {
            notification.remove();
        }, 300);
    }, 5000);
}

function showSuccess(message) {
    showNotification(message, 'success');
}

function showError(message) {
    showNotification(message, 'error');
}

function showLoading(message = 'Loading...') {
    showNotification(message, 'info');
}

function hideLoading() {
    const notifications = document.querySelectorAll('.notification');
    notifications.forEach(n => n.remove());
}





function openAuthModal(mode = 'signup') {
    const modal = document.getElementById('authModal');
    if (modal) {
        modal.classList.add('active');
        switchAuthTab(mode);
        

        document.body.style.overflow = 'hidden';
        

        setTimeout(() => {
            const firstInput = modal.querySelector(`#${mode}Form input[type="email"]`);
            if (firstInput) {
                firstInput.focus();
            }
        }, 100);
        
        console.log(`Opened auth modal in ${mode} mode`);
    } else {
        console.error('Auth modal not found!');
    }
}

function closeAuthModal() {
    const modal = document.getElementById('authModal');
    if (modal) {
        modal.classList.remove('active');
        

        document.body.style.overflow = '';
        

        clearAuthForms();
        
        console.log('Closed auth modal');
    }
}

function switchAuthTab(mode) {
    const tabs = document.querySelectorAll('.auth-tab');
    const forms = document.querySelectorAll('.auth-form');
    

    tabs.forEach(tab => {
        const isSignupTab = tab.textContent.toLowerCase().includes('account');
        const isLoginTab = tab.textContent.toLowerCase().includes('sign in');
        
        if ((mode === 'signup' && isSignupTab) || (mode === 'login' && isLoginTab)) {
            tab.classList.add('active');
        } else {
            tab.classList.remove('active');
        }
    });
    

    forms.forEach(form => {
        if (form.id === `${mode}Form`) {
            form.classList.add('active');
        } else {
            form.classList.remove('active');
        }
    });
    
    console.log(`Switched to ${mode} tab`);
}

function clearAuthForms() {

    document.querySelectorAll('#authModal input').forEach(input => {
        if (input.type !== 'checkbox') {
            input.value = '';
        } else {
            input.checked = false;
        }
    });
    
    console.log('Cleared auth forms');
}





function handleSignup(event) {
    event.preventDefault();
    
    const email = document.getElementById('signupEmail').value;
    const username = document.getElementById('signupUsername').value;
    const password = document.getElementById('signupPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    

    if (!email || !password || !confirmPassword) {
        showError('Please fill in all required fields');
        return;
    }
    
    if (password !== confirmPassword) {
        showError('Passwords do not match');
        return;
    }
    
    if (password.length < 8) {
        showError('Password must be at least 8 characters long');
        return;
    }
    

    const hasUppercase = /[A-Z]/.test(password);
    const hasLowercase = /[a-z]/.test(password);
    const hasNumber = /\d/.test(password);
    const hasSpecial = /[!@#$%^&*(),.?":{}|<>]/.test(password);
    
    if (!hasUppercase || !hasLowercase || !hasNumber || !hasSpecial) {
        showError('Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character');
        return;
    }
    
    showLoading('Creating account...');
    
    const requestData = {
        email: email,
        password: password,
        confirmPassword: confirmPassword,
        firstName: username || email.split('@')[0],
        lastName: ''
    };
    

    fetch('/api/auth/register', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestData)
    })
    .then(response => {
        return response.json().then(data => {
            if (!response.ok) {
                throw new Error(data.error || `HTTP error! status: ${response.status}`);
            }
            return data;
        });
    })
    .then(data => {
        hideLoading();
        
        if (data.success) {
            if (data.requiresVerification) {
                showSuccess('Account created! Please check your email for the verification code.');

                sessionStorage.setItem('pendingVerificationEmail', email);

                setTimeout(() => {
                    window.location.href = '/verify-email.html?email=' + encodeURIComponent(email);
                }, 1500);
            } else {
                showSuccess('Account created successfully! You can now sign in.');
                closeAuthModal();

                setTimeout(() => {
                    openAuthModal('login');
                }, 1500);
            }
        } else {
            showError(data.message || data.error || 'Registration failed. Please try again.');
        }
    })
    .catch(error => {
        hideLoading();
        console.error('Signup error:', error);
        showError(error.message || 'Registration failed. Please check your input and try again.');
    });
}

function handleLogin(event) {
    event.preventDefault();
    
    const email = document.getElementById('loginEmail').value;
    const password = document.getElementById('loginPassword').value;
    

    if (!email || !password) {
        showError('Please enter both email and password');
        return;
    }
    
    showLoading('Signing in...');
    
    const requestData = {
        email: email,
        password: password
    };
    

    fetch('/api/auth/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestData)
    })
    .then(response => {
        return response.json().then(data => {
            if (!response.ok) {
                throw new Error(data.error || `HTTP error! status: ${response.status}`);
            }
            return data;
        });
    })
    .then(data => {
        hideLoading();
        
        if (data.success) {
            showSuccess('Login successful! Redirecting...');
            closeAuthModal();
            

            setTimeout(() => {
                window.location.href = data.redirectUrl || '/chat.html';
            }, 1000);
        } else {
            if (data.errorCode === 'EMAIL_NOT_VERIFIED' && data.requiresVerification) {
                showError(data.error);

                setTimeout(() => {
                    window.location.href = '/verify-email.html?email=' + encodeURIComponent(email);
                }, 2000);
            } else {
                showError(data.message || data.error || 'Login failed. Please check your credentials.');
            }
        }
    })
    .catch(error => {
        hideLoading();
        console.error('Login error:', error);
        showError(error.message || 'Login failed. Please check your credentials and try again.');
    });
}





function triggerLogin() {
    console.log('Triggering login');
    openAuthModal('login');
}

function triggerSignup() {
    console.log('Triggering signup');
    openAuthModal('signup');
}

function triggerInstantAccess() {
    console.log('Triggering instant access');
    openAuthModal('signup');
}

function triggerFinalSignup() {
    console.log('Triggering final signup CTA');
    openAuthModal('signup');
}

function showDemo() {
    console.log('Showing demo');
    showNotification('Interactive demo coming soon! For now, try the live chat.', 'info');
    setTimeout(() => {
        openAuthModal('signup');
    }, 1500);
}

function triggerDemoChat() {
    console.log('Triggering demo chat');
    const input = document.getElementById('demoInput');
    if (input && input.value.trim()) {
        showNotification('Great idea! Sign up to start creating with AI.', 'success');
        setTimeout(() => {
            openAuthModal('signup');
        }, 1000);
    } else {
        showNotification('Enter a prompt first, then sign up to generate!', 'info');
    }
}


function selectPlan(planType) {
    console.log(`Selected plan: ${planType}`);
    openAuthModal('signup');
}


function handleGoogleAuth(mode) {
    console.log(`Google ${mode} not implemented yet`);
    showNotification('Google authentication coming soon! Please use email signup.', 'info');
}


function showForgotPassword() {
    showNotification('Password recovery feature coming soon!', 'info');
}


window.addEventListener('beforeunload', function() {

    if (countdownInterval) clearInterval(countdownInterval);
    if (liveFeedInterval) clearInterval(liveFeedInterval);
    if (testimonialInterval) clearInterval(testimonialInterval);
});


function initializeNeuralMesh() {
    const canvas = document.getElementById('neuralMesh');
    if (!canvas) return;
    
    const ctx = canvas.getContext('2d');
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
    

    let frame = 0;
    function animate() {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        

        const gradient = ctx.createLinearGradient(0, 0, canvas.width, canvas.height);
        gradient.addColorStop(0, `hsla(${240 + Math.sin(frame * 0.01) * 20}, 50%, 20%, 0.1)`);
        gradient.addColorStop(1, `hsla(${260 + Math.cos(frame * 0.008) * 20}, 50%, 30%, 0.05)`);
        
        ctx.fillStyle = gradient;
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        
        frame++;
        requestAnimationFrame(animate);
    }
    
    animate();
    

    window.addEventListener('resize', () => {
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;
    });
}


function initializeParticles() {
    const container = document.getElementById('particles');
    if (!container) return;
    

    for (let i = 0; i < 20; i++) {
        const particle = document.createElement('div');
        particle.className = 'particle';
        particle.style.cssText = `
            position: absolute;
            width: 2px;
            height: 2px;
            background: rgba(99, 102, 241, 0.5);
            border-radius: 50%;
            left: ${Math.random() * 100}%;
            top: ${Math.random() * 100}%;
            animation: float ${5 + Math.random() * 10}s infinite linear;
        `;
        container.appendChild(particle);
    }
}

console.log('🎯 VFChat Landing Page: LOADED');
