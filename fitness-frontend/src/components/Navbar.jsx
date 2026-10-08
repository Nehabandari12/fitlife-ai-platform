import React, { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Menu, Search, X, Activity, TrendingUp } from 'lucide-react'
import { getActivities } from '../services/api'
import SignupModal from './SignupModal'

const Navbar = ({ isAuthenticated, tokenData, onLogin, onLogout }) => {
  const [isOpen, setIsOpen] = useState(false)
  const [isSearchOpen, setIsSearchOpen] = useState(false)
  const [isSignupOpen, setIsSignupOpen] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState([])
  const [allActivities, setAllActivities] = useState([])
  const navigate = useNavigate()

  useEffect(() => {
    if (isAuthenticated) {
      fetchAllActivities()
    }
  }, [isAuthenticated])

  const fetchAllActivities = async () => {
    try {
      const response = await getActivities()
      setAllActivities(response.data)
    } catch (error) {
      console.error('Error fetching activities:', error)
    }
  }

  const handleLoginClick = (e) => {
    e.preventDefault();
    console.log("Navbar login clicked");
    if (onLogin) {
      onLogin();
    } else {
      console.error("onLogin function is not provided");
    }
  };

  const handleSignupClick = (e) => {
    e.preventDefault();
    console.log("Navbar signup clicked");
    setIsSignupOpen(true);
  };

  const handleSignupSuccess = () => {
    setIsSignupOpen(false);
    // Trigger login after successful signup
    if (onLogin) {
      onLogin();
    }
  };

  const handleSearch = (query) => {
    setSearchQuery(query)
    
    if (query.trim() === '') {
      setSearchResults([])
      return
    }

    const filtered = allActivities.filter(activity => 
      activity.type.toLowerCase().includes(query.toLowerCase()) ||
      activity.duration?.toString().includes(query) ||
      activity.caloriesBurned?.toString().includes(query) ||
      (activity.createdAt && new Date(activity.createdAt).toLocaleDateString().includes(query))
    )

    setSearchResults(filtered.slice(0, 5))
  }

  const handleSearchResultClick = (activityId) => {
    navigate(`/activities/${activityId}`)
    setIsSearchOpen(false)
    setSearchQuery('')
    setSearchResults([])
  }

  const closeSearch = () => {
    setIsSearchOpen(false)
    setSearchQuery('')
    setSearchResults([])
  }

  const handleRecommendationClick = () => {
    navigate('/complete-recommendation')
  }

  return (
    <>
      <div className='fixed top-0 left-0 z-50 w-full flex items-center justify-between px-6 md:px-16 lg:px-36 py-5 bg-black/70 backdrop-blur'>
        <Link to='/' className='text-white font-bold text-xl'>
          FitLife
        </Link>

        {/* Desktop Navigation */}
        <div className='hidden md:flex gap-8 items-center'>
          <Link 
            to='/'
            className='text-white hover:text-gray-300 transition'
          >
            Home
          </Link>
          {isAuthenticated && (
            <>
              <Link 
                to='/all-activities'
                className='text-white hover:text-gray-300 transition flex items-center gap-2'
              >
                <Activity size={18} />
                All Activities
              </Link>
              <button
                onClick={handleRecommendationClick}
                className='text-white hover:text-gray-300 transition flex items-center gap-2'
              >
                <TrendingUp size={18} />
                Get Recommendations
              </button>
            </>
          )}
        </div>

        <div className='flex items-center gap-4'>
          {isAuthenticated && (
            <button
              onClick={() => setIsSearchOpen(!isSearchOpen)}
              className='hidden md:block'
            >
              <Search className='w-6 h-6 cursor-pointer text-white hover:text-blue-400 transition'/>
            </button>
          )}
          
          {
            !isAuthenticated ? (
              <div className='flex items-center gap-3'>
                <button 
                  onClick={handleLoginClick}
                  type="button"
                  className='px-4 py-1 sm:px-6 sm:py-2 bg-transparent border-2 border-blue-600 text-blue-400 hover:bg-blue-600 hover:text-white transition rounded-full font-medium cursor-pointer'
                >
                  Login
                </button>
                <button 
                  onClick={handleSignupClick}
                  type="button"
                  className='px-4 py-1 sm:px-6 sm:py-2 bg-blue-600 hover:bg-blue-700 transition rounded-full font-medium cursor-pointer text-white border-none'
                >
                  Sign Up
                </button>
              </div>
            ) : (
              <div className='relative group'>
                <div className='w-10 h-10 rounded-full bg-blue-600 flex items-center justify-center text-white font-bold cursor-pointer'>
                  {tokenData?.email?.[0]?.toUpperCase() || 'U'}
                </div>
                <div className='absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 pointer-events-none group-hover:pointer-events-auto'>
                  <div className='p-4 border-b border-gray-200'>
                    <p className='text-sm font-medium text-gray-900 truncate'>{tokenData?.email || 'User'}</p>
                  </div>
                  <button
                    onClick={(e) => {
                      e.preventDefault();
                      if (onLogout) onLogout();
                    }}
                    type="button"
                    className='w-full px-4 py-2 text-left text-sm text-red-600 hover:bg-red-50 transition rounded-b-lg border-none bg-transparent cursor-pointer'
                  >
                    Logout
                  </button>
                </div>
              </div>
            )
          }
          
          <Menu className='md:hidden w-8 h-8 cursor-pointer text-white' onClick={() => setIsOpen(!isOpen)}/>
        </div>

        {/* Mobile Menu */}
        {isOpen && (
          <div className='fixed top-0 left-0 w-full h-screen bg-black/95 flex flex-col items-center justify-center gap-8 z-40'>
            <X 
              className='absolute top-6 right-6 w-8 h-8 cursor-pointer text-white' 
              onClick={() => setIsOpen(false)}
            />
            <Link 
              to='/'
              onClick={() => {window.scrollTo(0,0); setIsOpen(false)}}
              className='text-white text-xl font-medium hover:text-gray-300'
            >
              Home
            </Link>
            {isAuthenticated && (
              <>
                <Link 
                  to='/all-activities'
                  onClick={() => {window.scrollTo(0,0); setIsOpen(false)}}
                  className='text-white text-xl font-medium hover:text-gray-300 flex items-center gap-2'
                >
                  <Activity size={24} />
                  All Activities
                </Link>
                <button
                  onClick={() => {
                    setIsOpen(false)
                    navigate('/complete-recommendation')
                  }}
                  className='text-white text-xl font-medium hover:text-gray-300 flex items-center gap-2'
                >
                  <TrendingUp size={24} />
                  Get Recommendations
                </button>
                <button
                  onClick={() => {
                    setIsOpen(false)
                    setIsSearchOpen(true)
                  }}
                  className='text-white text-xl font-medium hover:text-gray-300 flex items-center gap-2'
                >
                  <Search size={24} />
                  Search Activities
                </button>
              </>
            )}
          </div>
        )}
      </div>

      {/* Search Modal */}
      {isSearchOpen && (
        <div className='fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-start justify-center pt-24'>
          <div className='w-full max-w-2xl mx-4'>
            <div className='bg-gray-800 rounded-2xl shadow-2xl border border-gray-700'>
              <div className='p-6 border-b border-gray-700'>
                <div className='flex items-center gap-4'>
                  <Search className='text-gray-400' size={24} />
                  <input
                    type='text'
                    value={searchQuery}
                    onChange={(e) => handleSearch(e.target.value)}
                    placeholder='Search by activity type, duration, calories...'
                    className='flex-1 bg-transparent text-white text-lg outline-none placeholder-gray-400'
                    autoFocus
                  />
                  <button onClick={closeSearch}>
                    <X className='text-gray-400 hover:text-white transition cursor-pointer' size={24} />
                  </button>
                </div>
              </div>

              <div className='max-h-96 overflow-y-auto'>
                {searchQuery && searchResults.length === 0 && (
                  <div className='p-8 text-center text-gray-400'>
                    No activities found matching "{searchQuery}"
                  </div>
                )}

                {searchResults.length > 0 && (
                  <div className='p-4'>
                    <p className='text-gray-400 text-sm mb-3 px-2'>
                      Found {searchResults.length} result{searchResults.length > 1 ? 's' : ''}
                    </p>
                    {searchResults.map((activity) => (
                      <div
                        key={activity.id}
                        onClick={() => handleSearchResultClick(activity.id)}
                        className='p-4 hover:bg-gray-700/50 rounded-lg cursor-pointer transition mb-2'
                      >
                        <div className='flex items-center justify-between'>
                          <div>
                            <h3 className='text-white font-semibold text-lg mb-1'>
                              {activity.type}
                            </h3>
                            <div className='flex gap-4 text-sm text-gray-400'>
                              <span>Duration: {activity.duration} min</span>
                              <span>Calories: {activity.caloriesBurned} kcal</span>
                            </div>
                            {activity.createdAt && (
                              <p className='text-xs text-gray-500 mt-1'>
                                {new Date(activity.createdAt).toLocaleDateString()}
                              </p>
                            )}
                          </div>
                          <Activity className='text-blue-400' size={24} />
                        </div>
                      </div>
                    ))}
                  </div>
                )}

                {!searchQuery && (
                  <div className='p-8 text-center text-gray-400'>
                    <Search className='mx-auto mb-4 text-gray-600' size={48} />
                    <p>Start typing to search your activities</p>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Signup Modal */}
      <SignupModal 
        isOpen={isSignupOpen}
        onClose={() => setIsSignupOpen(false)}
        onSignupSuccess={handleSignupSuccess}
      />
    </>
  )
}

export default Navbar