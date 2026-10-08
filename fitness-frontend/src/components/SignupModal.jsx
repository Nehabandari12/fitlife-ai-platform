import React, { useState } from 'react';
import { X, UserPlus, Mail, Lock, User, CheckCircle, AlertCircle, Eye, EyeOff } from 'lucide-react';
import { API_URL } from '../services/api';

const SignupModal = ({ isOpen, onClose, onSignupSuccess }) => {
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    firstName: '',
    lastName: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [fieldErrors, setFieldErrors] = useState({});

  const validateForm = () => {
    const errors = {};
    
    if (!formData.email) {
      errors.email = 'Email is required';
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      errors.email = 'Email is invalid';
    }
    
    if (!formData.password) {
      errors.password = 'Password is required';
    } else if (formData.password.length < 8) {
      errors.password = 'Password must be at least 8 characters';
    }
    
    if (!formData.firstName) {
      errors.firstName = 'First name is required';
    }
    
    if (!formData.lastName) {
      errors.lastName = 'Last name is required';
    }
    
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    if (fieldErrors[name]) {
      setFieldErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    setLoading(true);
    setError('');

    try {
      const response = await fetch(`${API_URL}/auth/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(formData)
      });

      const responseText = await response.text();

      // Check if registration was successful
      if (response.ok) {
        // Success response: "User registered. Please log in."
        setSuccess(true);
        setFormData({
          email: '',
          password: '',
          firstName: '',
          lastName: ''
        });

        // Wait 2 seconds before redirecting to login
        setTimeout(() => {
          setSuccess(false);
          onClose(); // Close the modal
          if (onSignupSuccess) {
            onSignupSuccess(); // Trigger login
          }
        }, 2000);
      } else {
        // Error response from server
        throw new Error(responseText || 'Registration failed');
      }

    } catch (err) {
      // Display error message
      setError(err.message || 'Something went wrong. Please try again.');
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
      <div className="bg-gradient-to-br from-gray-800 to-gray-900 rounded-2xl border border-gray-700 w-full max-w-md shadow-2xl relative">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-gray-400 hover:text-white transition"
          disabled={loading || success}
        >
          <X size={24} />
        </button>

        <div className="p-8 pb-4">
          <div className="w-16 h-16 bg-gradient-to-br from-blue-500 to-purple-600 rounded-full flex items-center justify-center mx-auto mb-4">
            <UserPlus className="text-white" size={32} />
          </div>
          <h2 className="text-3xl font-bold text-white text-center mb-2">Create Account</h2>
          <p className="text-gray-400 text-center">Join FitLife today!</p>
        </div>

        {success && (
          <div className="mx-8 mb-4 p-4 bg-green-500/20 border border-green-500/50 rounded-lg flex items-center gap-3 animate-pulse">
            <CheckCircle className="text-green-400 flex-shrink-0" size={24} />
            <div>
              <p className="text-green-400 font-medium">Registration Successful!</p>
              <p className="text-green-300 text-sm">Redirecting to login...</p>
            </div>
          </div>
        )}

        {error && (
          <div className="mx-8 mb-4 p-4 bg-red-500/20 border border-red-500/50 rounded-lg flex items-start gap-3">
            <AlertCircle className="text-red-400 flex-shrink-0 mt-0.5" size={20} />
            <p className="text-red-400 text-sm">{error}</p>
          </div>
        )}

        <div className="p-8 pt-4">
          <div className="space-y-4">
            <div>
              <label className="block text-gray-300 text-sm font-medium mb-2">
                First Name
              </label>
              <div className="relative">
                <User className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                <input
                  type="text"
                  name="firstName"
                  value={formData.firstName}
                  onChange={handleChange}
                  className={`w-full pl-11 pr-4 py-3 bg-gray-700/50 border ${
                    fieldErrors.firstName ? 'border-red-500' : 'border-gray-600'
                  } rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-blue-500 transition`}
                  placeholder="John"
                  disabled={loading || success}
                />
              </div>
              {fieldErrors.firstName && (
                <p className="text-red-400 text-xs mt-1">{fieldErrors.firstName}</p>
              )}
            </div>

            <div>
              <label className="block text-gray-300 text-sm font-medium mb-2">
                Last Name
              </label>
              <div className="relative">
                <User className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                <input
                  type="text"
                  name="lastName"
                  value={formData.lastName}
                  onChange={handleChange}
                  className={`w-full pl-11 pr-4 py-3 bg-gray-700/50 border ${
                    fieldErrors.lastName ? 'border-red-500' : 'border-gray-600'
                  } rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-blue-500 transition`}
                  placeholder="Doe"
                  disabled={loading || success}
                />
              </div>
              {fieldErrors.lastName && (
                <p className="text-red-400 text-xs mt-1">{fieldErrors.lastName}</p>
              )}
            </div>

            <div>
              <label className="block text-gray-300 text-sm font-medium mb-2">
                Email
              </label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleChange}
                  className={`w-full pl-11 pr-4 py-3 bg-gray-700/50 border ${
                    fieldErrors.email ? 'border-red-500' : 'border-gray-600'
                  } rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-blue-500 transition`}
                  placeholder="john@example.com"
                  disabled={loading || success}
                />
              </div>
              {fieldErrors.email && (
                <p className="text-red-400 text-xs mt-1">{fieldErrors.email}</p>
              )}
            </div>

            <div>
              <label className="block text-gray-300 text-sm font-medium mb-2">
                Password
              </label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                <input
                  type={showPassword ? 'text' : 'password'}
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  className={`w-full pl-11 pr-12 py-3 bg-gray-700/50 border ${
                    fieldErrors.password ? 'border-red-500' : 'border-gray-600'
                  } rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-blue-500 transition`}
                  placeholder="••••••••"
                  disabled={loading || success}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-white transition"
                  disabled={loading || success}
                >
                  {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
                </button>
              </div>
              {fieldErrors.password && (
                <p className="text-red-400 text-xs mt-1">{fieldErrors.password}</p>
              )}
              <p className="text-gray-500 text-xs mt-1">Must be at least 8 characters</p>
            </div>
          </div>

          <button
            onClick={handleSubmit}
            disabled={loading || success}
            className="w-full mt-6 py-3 bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white font-medium rounded-lg transition disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
          >
            {loading ? (
              <>
                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                Creating Account...
              </>
            ) : success ? (
              <>
                <CheckCircle size={20} />
                Success!
              </>
            ) : (
              <>
                <UserPlus size={20} />
                Sign Up
              </>
            )}
          </button>

          <p className="text-center text-gray-400 text-sm mt-4">
            Already have an account?{' '}
            <button
              type="button"
              onClick={() => {
                onClose();
                if (onSignupSuccess) {
                  onSignupSuccess();
                }
              }}
              className="text-blue-400 hover:text-blue-300 font-medium transition"
              disabled={loading || success}
            >
              Login here
            </button>
          </p>
        </div>
      </div>
    </div>
  );
};

export default SignupModal;