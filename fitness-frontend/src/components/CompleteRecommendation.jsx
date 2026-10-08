import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, TrendingUp, Zap, Shield, Target, Activity, Calendar, Users } from 'lucide-react';
import { getUserRecommendation } from '../services/api';

const CompleteRecommendation = ({ userId }) => {
  const [recommendation, setRecommendation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    fetchRecommendation();
  }, [userId]);

  const fetchRecommendation = async () => {
    try {
      setLoading(true);
      const response = await getUserRecommendation(userId);
      setRecommendation(response.data);
    } catch (err) {
      setError(err.response?.status === 404
        ? 'No recommendations yet. Log an activity first.'
        : 'Failed to fetch recommendation');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-900 flex items-center justify-center">
        <div className="text-center">
          <div className="relative w-32 h-32 mx-auto mb-8">
            <div className="absolute inset-0 rounded-full border-4 border-blue-500/30 animate-ping"></div>
            <div className="absolute inset-2 rounded-full border-4 border-blue-400/40 animate-pulse"></div>
            <div className="absolute inset-4 rounded-full border-4 border-blue-300/50 animate-spin"></div>
            <div className="absolute inset-0 flex items-center justify-center">
              <Activity className="text-blue-400 animate-bounce" size={48} />
            </div>
          </div>
          <h2 className="text-2xl font-bold text-white mb-2">Analyzing Your Fitness Data</h2>
          <p className="text-gray-400">Generating personalized recommendations...</p>
          <div className="mt-6 flex justify-center gap-2">
            <div className="w-2 h-2 bg-blue-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }}></div>
            <div className="w-2 h-2 bg-blue-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }}></div>
            <div className="w-2 h-2 bg-blue-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }}></div>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-900 py-8 px-4">
        <div className="max-w-4xl mx-auto">
          <button
            onClick={() => navigate('/')}
            className="flex items-center gap-2 text-blue-400 hover:text-blue-300 mb-6 transition"
          >
            <ArrowLeft size={20} />
            Back to Home
          </button>
          <div className="bg-red-500/10 border border-red-500/50 rounded-xl p-8 text-center">
            <p className="text-red-400 text-lg">Error: {error}</p>
          </div>
        </div>
      </div>
    );
  }

  if (!recommendation) {
    return (
      <div className="min-h-screen bg-gray-900 py-8 px-4">
        <div className="max-w-4xl mx-auto">
          <button
            onClick={() => navigate('/')}
            className="flex items-center gap-2 text-blue-400 hover:text-blue-300 mb-6 transition"
          >
            <ArrowLeft size={20} />
            Back to Home
          </button>
          <div className="bg-gray-800/50 border border-gray-700 rounded-xl p-8 text-center">
            <Activity className="mx-auto mb-4 text-gray-600" size={64} />
            <p className="text-gray-400 text-lg">No recommendations available yet. Complete some activities to get personalized insights!</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-900 py-8 px-4">
      <div className="max-w-6xl mx-auto">
        <button
          onClick={() => navigate('/')}
          className="flex items-center gap-2 text-blue-400 hover:text-blue-300 mb-6 transition"
        >
          <ArrowLeft size={20} />
          Back to Home
        </button>

        {/* Header */}
        <div className="bg-gradient-to-br from-gray-800 to-gray-900 rounded-2xl border border-gray-700 p-8 shadow-2xl mb-6">
          <div className="flex items-center gap-4 mb-2">
            <div className="w-16 h-16 bg-gradient-to-br from-blue-500 to-purple-600 rounded-full flex items-center justify-center">
              <TrendingUp className="text-white" size={32} />
            </div>
            <div>
              <h1 className="text-4xl font-bold text-white">Your Complete Fitness Analysis</h1>
              <p className="text-gray-400 mt-1">Comprehensive insights from all your activities</p>
            </div>
          </div>
          <div className="mt-4 flex items-center gap-4">
            <div className="flex items-center gap-2 text-gray-400">
              <Users size={18} />
              <span className="text-sm">{recommendation.type || 'USER_SUMMARY'}</span>
            </div>
            {recommendation.createdAt && (
              <div className="flex items-center gap-2 text-gray-400">
                <Calendar size={18} />
                <span className="text-sm">
                  Generated on {new Date(recommendation.createdAt).toLocaleDateString('en-US', {
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit'
                  })}
                </span>
              </div>
            )}
          </div>
        </div>

        {/* Main Content */}
        <div className="bg-gradient-to-br from-gray-800 to-gray-900 rounded-2xl border border-gray-700 p-8 shadow-2xl">
          {/* Overall Recommendation */}
          <div className="bg-gray-800/50 rounded-xl p-6 mb-6 border border-gray-700">
            <h2 className="text-2xl font-bold text-white mb-4 flex items-center gap-2">
              <Target className="text-blue-400" size={24} />
              Overall Analysis
            </h2>
            <div className="text-gray-300 whitespace-pre-line leading-relaxed">
              {recommendation.recommendation}
            </div>
          </div>

          {/* Improvements Section */}
          {recommendation.improvements && recommendation.improvements.length > 0 && (
            <div className="bg-gradient-to-br from-orange-500/10 to-red-500/10 rounded-xl p-6 mb-6 border border-orange-500/30">
              <h2 className="text-2xl font-bold text-white mb-4 flex items-center gap-2">
                <Zap className="text-orange-400" size={24} />
                Areas for Improvement
              </h2>
              <div className="space-y-4">
                {recommendation.improvements.map((improvement, index) => (
                  <div key={index} className="flex gap-4">
                    <div className="flex-shrink-0 w-8 h-8 bg-orange-500/20 rounded-full flex items-center justify-center text-orange-400 font-bold">
                      {index + 1}
                    </div>
                    <p className="text-gray-300 flex-1 pt-1">{improvement}</p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Suggestions Section */}
          {recommendation.suggestions && recommendation.suggestions.length > 0 && (
            <div className="bg-gradient-to-br from-green-500/10 to-emerald-500/10 rounded-xl p-6 mb-6 border border-green-500/30">
              <h2 className="text-2xl font-bold text-white mb-4 flex items-center gap-2">
                <Target className="text-green-400" size={24} />
                Workout Suggestions
              </h2>
              <div className="space-y-4">
                {recommendation.suggestions.map((suggestion, index) => (
                  <div key={index} className="bg-gray-800/50 rounded-lg p-5 border border-gray-700">
                    <div className="flex gap-4">
                      <div className="flex-shrink-0 w-8 h-8 bg-green-500/20 rounded-full flex items-center justify-center text-green-400 font-bold">
                        {index + 1}
                      </div>
                      <p className="text-gray-300 flex-1 pt-1">{suggestion}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Safety Section */}
          {recommendation.safety && recommendation.safety.length > 0 && (
            <div className="bg-gradient-to-br from-blue-500/10 to-cyan-500/10 rounded-xl p-6 border border-blue-500/30">
              <h2 className="text-2xl font-bold text-white mb-4 flex items-center gap-2">
                <Shield className="text-blue-400" size={24} />
                Safety Guidelines
              </h2>
              <div className="space-y-3">
                {recommendation.safety.map((item, index) => (
                  <div key={index} className="flex gap-3 items-start">
                    <Shield className="text-blue-400 flex-shrink-0 mt-1" size={18} />
                    <p className="text-gray-300">{item}</p>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default CompleteRecommendation;